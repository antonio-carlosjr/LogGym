package com.loggym.data.seed

import com.loggym.domain.model.MuscleGroup

data class NativeExercise(
    val key: String,
    val name: String,
    val muscleGroups: Set<MuscleGroup>,
    val description: String? = null,
)

/**
 * Biblioteca nativa inicial.
 * PROVISÓRIO: o conteúdo definitivo é uma decisão de produto pendente (seção 17, item 1).
 * As chaves são estáveis e referenciadas pelos templates; não renomeie uma chave já publicada.
 */
object NativeExercises {
    private val PEITO = setOf(MuscleGroup.CHEST)
    private val COSTAS = setOf(MuscleGroup.BACK)
    private val OMBROS = setOf(MuscleGroup.SHOULDERS)
    private val BICEPS = setOf(MuscleGroup.BICEPS)
    private val TRICEPS = setOf(MuscleGroup.TRICEPS)
    private val PERNAS = setOf(MuscleGroup.QUADS)
    private val GLUTEOS = setOf(MuscleGroup.GLUTES)
    private val PANTURRILHAS = setOf(MuscleGroup.CALVES)
    private val ABDOMEN = setOf(MuscleGroup.ABS)

    val all: List<NativeExercise> = listOf(
        NativeExercise("supino_reto", "Supino reto com barra", PEITO),
        NativeExercise("supino_inclinado_halteres", "Supino inclinado com halteres", PEITO),
        NativeExercise("crucifixo_maquina", "Crucifixo na máquina", PEITO),
        NativeExercise("crossover", "Crossover na polia", PEITO),

        NativeExercise("puxada_frontal", "Puxada frontal", COSTAS),
        NativeExercise("barra_fixa", "Barra fixa", COSTAS),
        NativeExercise("remada_curvada", "Remada curvada com barra", COSTAS),
        NativeExercise("remada_baixa", "Remada baixa na polia", COSTAS),
        NativeExercise("levantamento_terra", "Levantamento terra", COSTAS),

        NativeExercise("desenvolvimento_halteres", "Desenvolvimento com halteres", OMBROS),
        NativeExercise("elevacao_lateral", "Elevação lateral", OMBROS),
        NativeExercise("crucifixo_inverso", "Crucifixo inverso", OMBROS),

        NativeExercise("rosca_direta", "Rosca direta com barra", BICEPS),
        NativeExercise("rosca_martelo", "Rosca martelo", BICEPS),

        NativeExercise("triceps_polia", "Tríceps na polia", TRICEPS),
        NativeExercise("triceps_frances", "Tríceps francês", TRICEPS),

        NativeExercise("agachamento_livre", "Agachamento livre", PERNAS),
        NativeExercise("leg_press", "Leg press 45°", PERNAS),
        NativeExercise("cadeira_extensora", "Cadeira extensora", PERNAS),
        NativeExercise("mesa_flexora", "Mesa flexora", PERNAS),
        NativeExercise("stiff", "Stiff", PERNAS),
        NativeExercise("afundo_halteres", "Afundo com halteres", PERNAS),

        NativeExercise("elevacao_pelvica", "Elevação pélvica", GLUTEOS),

        NativeExercise("panturrilha_em_pe", "Panturrilha em pé", PANTURRILHAS),

        NativeExercise("abdominal_polia", "Abdominal na polia", ABDOMEN),
    )
}
