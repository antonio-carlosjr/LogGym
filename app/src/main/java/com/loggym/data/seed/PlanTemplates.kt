package com.loggym.data.seed

import com.loggym.domain.model.ExerciseTarget
import com.loggym.domain.model.ExerciseTemplate
import com.loggym.domain.model.PlanTemplate
import com.loggym.domain.model.WorkoutTemplate
import java.time.Duration

/**
 * Planos pré-definidos (RF-35, RF-36, RF-144): público iniciante a intermediário,
 * com 4 a 6 exercícios e 12 a 20 séries por treino. Compostos primeiro.
 * Conteúdo definido pela equipe (decisão S1-04, itens 2.1 a 2.3).
 */
object PlanTemplates {

    private fun ex(key: String, sets: Int, repMin: Int, repMax: Int, restSeconds: Long) =
        ExerciseTemplate(key, ExerciseTarget(sets, repMin, repMax, Duration.ofSeconds(restSeconds)))

    private val pushPullLegs = PlanTemplate(
        key = "ppl",
        name = "Push / Pull / Legs",
        description = "Empurrar, puxar e pernas em três divisões.",
        workouts = listOf(
            WorkoutTemplate(
                "Push",
                listOf(
                    ex("supino_reto", 3, 6, 10, 180),
                    ex("supino_inclinado_halteres", 3, 8, 12, 120),
                    ex("desenvolvimento_halteres", 3, 8, 12, 120),
                    ex("elevacao_lateral", 3, 12, 15, 60),
                    ex("triceps_polia", 3, 10, 15, 60),
                ),
            ),
            WorkoutTemplate(
                "Pull",
                listOf(
                    ex("puxada_frontal", 3, 8, 12, 120),
                    ex("remada_curvada", 3, 6, 10, 150),
                    ex("remada_baixa", 3, 10, 12, 90),
                    ex("crucifixo_inverso", 3, 12, 15, 60),
                    ex("rosca_direta", 3, 8, 12, 60),
                ),
            ),
            WorkoutTemplate(
                "Legs",
                listOf(
                    ex("agachamento_livre", 3, 6, 10, 180),
                    ex("leg_press", 3, 10, 12, 120),
                    ex("stiff", 3, 8, 12, 120),
                    ex("mesa_flexora", 3, 12, 15, 60),
                    ex("panturrilha_em_pe", 4, 10, 15, 60),
                ),
            ),
        ),
    )

    private val upperLower = PlanTemplate(
        key = "upper_lower",
        name = "Upper / Lower",
        description = "Superiores e inferiores alternados em quatro divisões.",
        workouts = listOf(
            WorkoutTemplate(
                "Upper A",
                listOf(
                    ex("supino_reto", 3, 6, 10, 180),
                    ex("remada_curvada", 3, 6, 10, 150),
                    ex("desenvolvimento_halteres", 3, 8, 12, 120),
                    ex("rosca_direta", 2, 10, 12, 60),
                    ex("triceps_polia", 2, 10, 12, 60),
                ),
            ),
            WorkoutTemplate(
                "Lower A",
                listOf(
                    ex("agachamento_livre", 3, 6, 10, 180),
                    ex("mesa_flexora", 3, 10, 12, 90),
                    ex("cadeira_extensora", 3, 12, 15, 60),
                    ex("panturrilha_em_pe", 3, 10, 15, 60),
                ),
            ),
            WorkoutTemplate(
                "Upper B",
                listOf(
                    ex("supino_inclinado_halteres", 3, 8, 12, 120),
                    ex("puxada_frontal", 3, 8, 12, 120),
                    ex("elevacao_lateral", 3, 12, 15, 60),
                    ex("rosca_martelo", 2, 10, 12, 60),
                    ex("triceps_frances", 2, 10, 12, 60),
                ),
            ),
            WorkoutTemplate(
                "Lower B",
                listOf(
                    ex("levantamento_terra", 3, 5, 8, 180),
                    ex("leg_press", 3, 10, 12, 120),
                    ex("afundo_halteres", 3, 10, 12, 90),
                    ex("abdominal_polia", 3, 12, 15, 60),
                ),
            ),
        ),
    )

    private val broSplit = PlanTemplate(
        key = "bro_split",
        name = "Bro Split",
        description = "Um grupo muscular por dia em cinco divisões.",
        workouts = listOf(
            WorkoutTemplate(
                "Peito",
                listOf(
                    ex("supino_reto", 4, 6, 10, 180),
                    ex("supino_inclinado_halteres", 3, 8, 12, 120),
                    ex("crucifixo_maquina", 3, 10, 15, 90),
                    ex("crossover", 3, 12, 15, 60),
                ),
            ),
            WorkoutTemplate(
                "Costas",
                listOf(
                    ex("barra_fixa", 3, 6, 10, 150),
                    ex("remada_curvada", 3, 6, 10, 150),
                    ex("puxada_frontal", 3, 10, 12, 90),
                    ex("remada_baixa", 3, 10, 12, 90),
                ),
            ),
            WorkoutTemplate(
                "Ombros",
                listOf(
                    ex("desenvolvimento_halteres", 4, 8, 12, 120),
                    ex("elevacao_lateral", 4, 12, 15, 60),
                    ex("crucifixo_inverso", 3, 12, 15, 60),
                    ex("encolhimento_halteres", 3, 10, 12, 90),
                ),
            ),
            WorkoutTemplate(
                "Braços",
                listOf(
                    ex("rosca_direta", 3, 8, 12, 90),
                    ex("triceps_frances", 3, 8, 12, 90),
                    ex("rosca_martelo", 3, 10, 12, 60),
                    ex("triceps_polia", 3, 10, 15, 60),
                ),
            ),
            WorkoutTemplate(
                "Pernas",
                listOf(
                    ex("agachamento_livre", 4, 6, 10, 180),
                    ex("leg_press", 3, 10, 12, 120),
                    ex("cadeira_extensora", 3, 12, 15, 60),
                    ex("mesa_flexora", 3, 12, 15, 60),
                    ex("elevacao_pelvica", 3, 8, 12, 90),
                    ex("panturrilha_em_pe", 4, 10, 15, 60),
                ),
            ),
        ),
    )

    private val fullBody = PlanTemplate(
        key = "full_body",
        name = "Full Body 3x",
        description = "Corpo inteiro em três treinos alternados, ideal para iniciantes.",
        workouts = listOf(
            WorkoutTemplate(
                "Full Body A",
                listOf(
                    ex("agachamento_livre", 3, 6, 10, 180),
                    ex("supino_reto", 3, 6, 10, 180),
                    ex("remada_curvada", 3, 8, 12, 120),
                    ex("desenvolvimento_halteres", 3, 8, 12, 120),
                    ex("abdominal_crunch", 3, 12, 15, 60),
                ),
            ),
            WorkoutTemplate(
                "Full Body B",
                listOf(
                    ex("levantamento_terra", 3, 5, 8, 180),
                    ex("supino_inclinado_halteres", 3, 8, 12, 120),
                    ex("puxada_frontal", 3, 8, 12, 120),
                    ex("elevacao_lateral", 3, 12, 15, 60),
                    ex("panturrilha_em_pe", 3, 10, 15, 60),
                ),
            ),
            WorkoutTemplate(
                "Full Body C",
                listOf(
                    ex("leg_press", 3, 10, 12, 120),
                    ex("paralelas", 3, 6, 12, 120),
                    ex("remada_unilateral", 3, 8, 12, 90),
                    ex("rosca_direta", 3, 8, 12, 60),
                    ex("triceps_corda", 3, 10, 15, 60),
                ),
            ),
        ),
    )

    val all: List<PlanTemplate> = listOf(fullBody, pushPullLegs, upperLower, broSplit)

    fun byKey(key: String): PlanTemplate? = all.firstOrNull { it.key == key }
}
