package com.loggym.e2e

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.loggym.domain.model.*
import com.loggym.ui.session.SessionSetRow
import com.loggym.ui.session.SetEntry
import com.loggym.ui.theme.LogGymTheme
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.Duration

@RunWith(AndroidJUnit4::class)
class Sprint7SetLayoutTest {
    @get:Rule val compose = createComposeRule(effectContext = object : MotionDurationScale {
        override val scaleFactor = 0f
    })
    @Test fun reflowPreservesFieldIdentityTextAndFocusWithMotionDisabled() {
        val width = mutableStateOf(344.dp)
        val scale = mutableStateOf(1f)
        val completed = mutableStateOf(false)
        var focused = 0
        var removed = 0
        val set = SessionSet(11, 0, null, null, false)
        val exercise = SessionExercise(1, 2, "Supino", emptySet(), 0,
            ExerciseTarget.Repetitions(1, 8, 12, Duration.ofSeconds(37)), listOf(set))
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale.value)) {
                LogGymTheme() {
                    var weight by remember { mutableStateOf("22,5") }
                    var reps by remember { mutableStateOf("8") }
                    Box(Modifier.width(width.value)) {
                        SessionSetRow(exercise, set.copy(isCompleted = completed.value), 1,
                            SetEntry(weight = weight, reps = reps), false, false,
                            onToggle = { completed.value = !completed.value }, onRemove = { removed++ }) {
                            OutlinedTextField(weight, { weight = it }, modifier = Modifier.testTag("weight")
                                .onFocusChanged { if (it.isFocused) focused++ }, singleLine = true)
                            OutlinedTextField(reps, { reps = it }, modifier = Modifier.testTag("reps"), singleLine = true)
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("weight").performClick()
        compose.onNodeWithTag("weight").performTextReplacement("30,5")
        compose.runOnIdle { width.value = 288.dp; scale.value = 2f }
        compose.onNodeWithTag("weight").assertIsFocused().assertTextContains("30,5")
        compose.onNodeWithTag("reps").assertTextContains("8")
        compose.runOnIdle { assertEquals(1, focused); assertFalse(completed.value) }
        compose.onNodeWithContentDescription("Concluir série 1 de Supino")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick().assertIsOn()
        compose.onNodeWithContentDescription("Remover série 1").assertDoesNotExist()
        compose.onNodeWithContentDescription("Concluir série 1 de Supino").performClick()
        compose.onNodeWithContentDescription("Remover série 1")
            .assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp).performClick()
        compose.runOnIdle { assertEquals(1, removed) }
    }
}
