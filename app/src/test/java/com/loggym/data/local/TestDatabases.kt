package com.loggym.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

val testContext: Context get() = ApplicationProvider.getApplicationContext()

val fixedClock: Clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC)

/** Banco em memória com a mesma carga inicial do app. */
fun inMemoryDatabase(): LogGymDatabase =
    Room.inMemoryDatabaseBuilder(testContext, LogGymDatabase::class.java)
        .addCallback(NativeExerciseSeedCallback())
        .build()

/** Banco em arquivo, para cenários que precisam fechar e reabrir o banco. */
fun fileDatabase(name: String): LogGymDatabase =
    Room.databaseBuilder(testContext, LogGymDatabase::class.java, name)
        .addCallback(NativeExerciseSeedCallback())
        .build()
