package com.loggym.data.backup

import com.loggym.domain.backup.BackupMode
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.io.OutputStream

class BackupLimitsTest {
    private val discard = object : OutputStream() {
        override fun write(value: Int) {}
        override fun write(bytes: ByteArray, offset: Int, length: Int) {}
    }
    private val header = BackupHeader(mode = BackupMode.FULL, createdAt = 1000, catalog = BackupFormat.catalog,
        floors = BackupFormat.tables.associateWith { 1L }, preferences = BackupPreferences("system", true))
    @Test fun exportRejectsTooManyRecordsInsteadOfTruncating() {
        val row = BackupRow("exercises", buildJsonObject {})
        val failure = runCatching { BackupFormat.write(discard, header, generateSequence { row }.take(BackupFormat.MAX_ROWS + 1)) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("limite"))
    }
    @Test fun exportRejectsTotalByteLimitWithIndividuallyValidRecords() {
        val row = BackupRow("exercises", buildJsonObject { put("description", "x".repeat(900_000)) })
        val failure = runCatching { BackupFormat.write(discard, header, generateSequence { row }.take(80)) }.exceptionOrNull()
        assertTrue(failure is IllegalArgumentException)
        assertTrue(failure!!.message!!.contains("tamanho"))
    }
}
