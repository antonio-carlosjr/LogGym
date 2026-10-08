package com.loggym

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/** Substitui a LogGymApplication pela aplicação de teste do Hilt nos testes instrumentados. */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(classLoader: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(classLoader, HiltTestApplication::class.java.name, context)
}
