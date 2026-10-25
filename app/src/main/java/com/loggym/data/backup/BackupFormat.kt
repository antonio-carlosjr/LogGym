package com.loggym.data.backup

import android.util.JsonReader
import android.util.JsonToken
import com.loggym.data.seed.NativeExercises
import com.loggym.data.local.Converters
import com.loggym.domain.backup.BackupMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest

@Serializable
data class BackupPreferences(val theme: String, val prefillEnabled: Boolean)
@Serializable
data class BackupHeader(val magic: String = "LOGGYM_BACKUP", val version: Int = 1,
    val mode: BackupMode, val createdAt: Long, val appVersion: String = "0.1.0",
    val catalog: String, val floors: Map<String, Long>, val preferences: BackupPreferences,
    val previousOnboarding: Boolean? = null)

/** DTO lógico independente das entidades Room; nomes de tabelas/colunas são whitelist local. */
data class BackupRow(val table: String, val values: JsonObject)
object BackupFormat {
    const val MAX_BYTES = 64L * 1024 * 1024
    const val MAX_LINE = 1024 * 1024
    const val MAX_ROWS = 500_000
    val json = Json { encodeDefaults = true; explicitNulls = true; ignoreUnknownKeys = false; isLenient = false }
    val tables = listOf("exercises", "plans", "workouts", "workout_exercises", "sessions", "session_exercises", "session_sets")
    fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    val catalog: String get() = sha(NativeExercises.all.joinToString("\n") {
        "${it.key}|${it.name}|${Converters.encodeMuscleGroups(it.muscleGroups)}|${it.description}"
    }.toByteArray(Charsets.UTF_8))

    fun write(output: OutputStream, header: BackupHeader, rows: Sequence<BackupRow>) {
        val digest = MessageDigest.getInstance("SHA-256")
        var size = 0L
        var count = 0
        val counts = tables.associateWith { 0L }.toMutableMap()
        fun line(type: String, data: JsonElement, hashed: Boolean = true) {
            val bytes = (json.encodeToString(buildJsonObject { put("recordType", type); put("data", data) }) + "\n").toByteArray(Charsets.UTF_8)
            require(bytes.size <= MAX_LINE && size + bytes.size <= MAX_BYTES) { "Limite de tamanho do backup excedido" }
            output.write(bytes); size += bytes.size
            if (hashed) digest.update(bytes)
        }
        line("header", json.encodeToJsonElement(header))
        var last = -1
        rows.forEach { row ->
            val index = tables.indexOf(row.table)
            require(index >= last && index >= 0 && ++count <= MAX_ROWS) { "Registros fora de ordem ou limite excedido" }
            last = index
            if (header.mode == BackupMode.WITHOUT_HISTORY) require(index < 4)
            counts[row.table] = counts.getValue(row.table) + 1
            line(row.table, row.values)
        }
        line("end", buildJsonObject {
            put("counts", json.encodeToJsonElement(counts))
            put("sha256", digest.digest().joinToString("") { "%02x".format(it) })
        }, false)
        output.flush()
    }

    fun read(input: InputStream, internal: Boolean = false, header: (BackupHeader) -> Unit,
        row: (BackupRow) -> Unit): BackupHeader {
        val digest = MessageDigest.getInstance("SHA-256")
        val counts = tables.associateWith { 0L }.toMutableMap()
        var size = 0L; var total = 0; var last = -1; var metadata: BackupHeader? = null; var ended = false
        val stream = BufferedInputStream(input)
        while (true) {
            val line = ByteArrayOutputStream()
            while (true) {
                val c = stream.read()
                if (c == -1) { require(line.size() == 0) { "Registro truncado" }; break }
                size++; require(size <= MAX_BYTES && line.size() < MAX_LINE) { "Limite de tamanho excedido" }
                line.write(c); if (c == 10) break
            }
            if (line.size() == 0) break
            require(!ended) { "Dados após o rodapé" }
            val bytes = line.toByteArray()
            val text = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
            checkKeys(text)
            val obj = json.parseToJsonElement(text).jsonObject
            require(obj.keys == setOf("recordType", "data"))
            val type = obj.getValue("recordType").jsonPrimitive.content
            require(obj.getValue("recordType").jsonPrimitive.isString)
            val data = obj.getValue("data")
            when {
                metadata == null -> {
                    require(type == "header")
                    require(data.jsonObject.keys == setOf("magic", "version", "mode", "createdAt", "appVersion", "catalog", "floors", "preferences", "previousOnboarding"))
                    require(data.jsonObject.getValue("preferences").jsonObject.keys == setOf("theme", "prefillEnabled"))
                    val fields = data.jsonObject
                    for (key in listOf("magic", "mode", "appVersion", "catalog")) require(fields.getValue(key).jsonPrimitive.isString)
                    for (key in listOf("version", "createdAt")) require(fields.getValue(key).jsonPrimitive.let { !it.isString && it.longOrNull != null })
                    require(fields.getValue("floors").jsonObject.values.all { it.jsonPrimitive.let { value -> !value.isString && value.longOrNull != null } })
                    val prefs = fields.getValue("preferences").jsonObject
                    require(prefs.getValue("theme").jsonPrimitive.isString)
                    require(prefs.getValue("prefillEnabled").jsonPrimitive.let { !it.isString && it.booleanOrNull != null })
                    require(fields.getValue("previousOnboarding").let { it == JsonNull || it.jsonPrimitive.let { p -> !p.isString && p.booleanOrNull != null } })
                    metadata = json.decodeFromJsonElement<BackupHeader>(data)
                    require(metadata.version == 1 && metadata.catalog == catalog) { "Versão ou catálogo incompatível" }
                    require(metadata.magic == if (internal) "LOGGYM_UNDO" else "LOGGYM_BACKUP") { "Arquivo não é um backup LogGym" }
                    require(internal || metadata.previousOnboarding == null)
                    require(metadata.floors.keys == tables.toSet() && metadata.floors.values.all { it >= 0 && it < Long.MAX_VALUE })
                    require(metadata.preferences.theme in listOf("system", "light", "dark"))
                    require(metadata.createdAt >= 0)
                    header(metadata)
                }
                type == "end" -> {
                    val footer = data.jsonObject
                    require(footer.keys == setOf("counts", "sha256"))
                    require(footer.getValue("counts").jsonObject.values.all { it.jsonPrimitive.let { value -> !value.isString && value.longOrNull != null } })
                    require(footer.getValue("sha256").jsonPrimitive.isString)
                    require(json.decodeFromJsonElement<Map<String, Long>>(footer.getValue("counts")) == counts) { "Contagens inválidas" }
                    require(footer.getValue("sha256").jsonPrimitive.content == digest.digest().joinToString("") { "%02x".format(it) }) { "Integridade inválida" }
                    ended = true
                }
                else -> {
                    val index = tables.indexOf(type)
                    require(index >= 0 && index >= last && ++total <= MAX_ROWS)
                    require(metadata.mode == BackupMode.FULL || index < 4) { "Backup sem histórico contém sessões" }
                    last = index; counts[type] = counts.getValue(type) + 1
                    row(BackupRow(type, data.jsonObject))
                }
            }
            if (!ended) digest.update(bytes)
        }
        require(ended) { "Backup incompleto" }
        return requireNotNull(metadata)
    }

    /** Não confiar no comportamento permissivo de propriedades repetidas do decodificador. */
    private fun checkKeys(text: String) {
        JsonReader(StringReader(text)).use { reader ->
            reader.isLenient = false
            fun visit(depth: Int) {
                require(depth <= 16)
                when (reader.peek()) {
                    JsonToken.BEGIN_OBJECT -> {
                        val names = hashSetOf<String>(); reader.beginObject()
                        while (reader.hasNext()) { require(names.add(reader.nextName())) { "Propriedade duplicada" }; visit(depth + 1) }
                        reader.endObject()
                    }
                    JsonToken.BEGIN_ARRAY -> { reader.beginArray(); while (reader.hasNext()) visit(depth + 1); reader.endArray() }
                    JsonToken.NULL -> reader.nextNull()
                    JsonToken.BOOLEAN -> reader.nextBoolean()
                    JsonToken.STRING, JsonToken.NUMBER -> reader.nextString()
                    else -> error("JSON inválido")
                }
            }
            visit(0); require(reader.peek() == JsonToken.END_DOCUMENT)
        }
    }
}
