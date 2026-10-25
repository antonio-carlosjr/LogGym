package com.loggym.data.backup

import kotlinx.serialization.json.*

/** Fixtures externas semanticamente alteradas precisam de checksum válido para testar a validação correta. */
internal fun rewriteBackupHeader(bytes: ByteArray, edit: (JsonObject) -> JsonObject): ByteArray {
    val lines = bytes.toString(Charsets.UTF_8).trimEnd('\n').split('\n').toMutableList()
    val first = BackupFormat.json.parseToJsonElement(lines.first()).jsonObject
    lines[0] = JsonObject(first + ("data" to edit(first.getValue("data").jsonObject))).toString()
    val body = lines.dropLast(1).joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
    val end = BackupFormat.json.parseToJsonElement(lines.last()).jsonObject
    val data = end.getValue("data").jsonObject
    val footer = JsonObject(end + ("data" to JsonObject(data + ("sha256" to JsonPrimitive(BackupFormat.sha(body))))))
    return body + (footer.toString() + "\n").toByteArray(Charsets.UTF_8)
}

internal fun s5Header(header: JsonObject): JsonObject = JsonObject(header + ("version" to JsonPrimitive(1)) +
    ("preferences" to JsonObject(header.getValue("preferences").jsonObject - "autoCompleteEnabled")))
