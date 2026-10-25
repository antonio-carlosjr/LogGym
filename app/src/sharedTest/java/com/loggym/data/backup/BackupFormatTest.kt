package com.loggym.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.domain.backup.BackupMode
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class BackupFormatTest {
    private fun header(mode: BackupMode = BackupMode.FULL) = BackupHeader(mode = mode, createdAt = 1000,
        catalog = BackupFormat.catalog, floors = BackupFormat.tables.associateWith { 100L }, preferences = BackupPreferences("dark", false))
    private fun encode(mode: BackupMode = BackupMode.FULL, rows: Sequence<BackupRow> = emptySequence()) = ByteArrayOutputStream().also { BackupFormat.write(it, header(mode), rows) }.toByteArray()
    private fun read(bytes: ByteArray) = BackupFormat.read(bytes.inputStream(), header = {}, row = {})
    @Test fun roundTripPreservesMetadataAndZeroNullAndUnicode() {
        val row = BackupRow("session_sets", buildJsonObject { put("weight_kg", 0.0); put("reps", 0); put("duration_seconds", JsonNull); put("name", "Execução 🏋") })
        var restored: BackupRow? = null
        val metadata = BackupFormat.read(encode(rows = sequenceOf(row)).inputStream(), header = {}, row = { restored = it })
        assertEquals(header(), metadata); assertEquals(row, restored)
    }
    @Test fun bothModalitiesRoundTrip() { BackupMode.entries.forEach { assertEquals(it, read(encode(it)).mode) } }
    @Test fun rejectsUnknownVersionAndCatalog() {
        val bytes = encode().toString(Charsets.UTF_8)
        for (bad in listOf(bytes.replace("\"version\":1", "\"version\":2"), bytes.replace(BackupFormat.catalog, "unknown")))
            assertTrue(runCatching { read(bad.toByteArray()) }.isFailure)
    }
    @Test fun requiresFooterAndChecksumAndNoTrailingData() {
        val bytes = encode()
        assertTrue(runCatching { read(bytes.copyOf(bytes.size - 2)) }.isFailure)
        assertTrue(runCatching { read(bytes.toString(Charsets.UTF_8).substringBeforeLast("{\"recordType\":\"end\"").toByteArray()) }.isFailure)
        assertTrue(runCatching { read(bytes + "{}\n".toByteArray()) }.isFailure)
        assertTrue(runCatching { read(bytes.toString(Charsets.UTF_8).replace("\"createdAt\":1000", "\"createdAt\":2000").toByteArray()) }.isFailure)
    }
    @Test fun rejectsDuplicateKeysMissingFieldsAndWrongTypes() {
        val text = encode().toString(Charsets.UTF_8)
        listOf(text.replace("\"version\":1", "\"version\":1,\"version\":1"),
            text.replace("\"magic\":\"LOGGYM_BACKUP\",", ""),
            text.replace("\"prefillEnabled\":false", "\"prefillEnabled\":\"false\""),
            text.replace("\"theme\":\"dark\"", "\"theme\":\"pink\""),
            text.replace("\"floors\":{", "\"unknown\":1,\"floors\":{")).forEach {
            assertTrue(runCatching { read(it.toByteArray()) }.isFailure)
        }
    }
    @Test fun rejectsOversizedRecordAndMalformedUtf8() {
        assertTrue(runCatching { encode(rows = sequenceOf(BackupRow("exercises", buildJsonObject { put("description", "x".repeat(BackupFormat.MAX_LINE)) }))) }.isFailure)
        assertTrue(runCatching { read(byteArrayOf(0xc3.toByte(), 0x28, 10)) }.isFailure)
    }
    @Test fun rejectsSessionsInWithoutHistoryAndUnorderedTables() {
        assertTrue(runCatching { encode(BackupMode.WITHOUT_HISTORY, sequenceOf(BackupRow("sessions", buildJsonObject {}))) }.isFailure)
        assertTrue(runCatching { encode(rows = sequenceOf(BackupRow("plans", buildJsonObject {}), BackupRow("exercises", buildJsonObject {}))) }.isFailure)
    }
}
