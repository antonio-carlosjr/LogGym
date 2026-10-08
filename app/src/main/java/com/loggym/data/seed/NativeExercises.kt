package com.loggym.data.seed

import com.loggym.domain.model.MuscleGroup
import com.loggym.domain.model.MuscleGroup.ABDUCTORS
import com.loggym.domain.model.MuscleGroup.ABS
import com.loggym.domain.model.MuscleGroup.ADDUCTORS
import com.loggym.domain.model.MuscleGroup.BACK
import com.loggym.domain.model.MuscleGroup.BICEPS
import com.loggym.domain.model.MuscleGroup.CALVES
import com.loggym.domain.model.MuscleGroup.CHEST
import com.loggym.domain.model.MuscleGroup.FOREARMS
import com.loggym.domain.model.MuscleGroup.GLUTES
import com.loggym.domain.model.MuscleGroup.HAMSTRINGS
import com.loggym.domain.model.MuscleGroup.QUADS
import com.loggym.domain.model.MuscleGroup.SHOULDERS
import com.loggym.domain.model.MuscleGroup.TRAPS
import com.loggym.domain.model.MuscleGroup.TRICEPS

data class NativeExercise(
    val key: String,
    val name: String,
    val muscleGroups: Set<MuscleGroup>,
    val description: String,
)

/**
 * Biblioteca nativa (decisão S1-04, itens 1.1 a 1.7): cerca de 60 exercícios de carga × repetições.
 * Nome no padrão "Movimento + variação + equipamento"; grupos em ordem de relevância.
 * As chaves são estáveis e referenciadas pelos templates. Uma chave publicada nunca muda (RF-148).
 */
object NativeExercises {

    private fun ex(key: String, name: String, vararg groups: MuscleGroup, description: String) =
        NativeExercise(key, name, groups.toSet(), description)

    val all: List<NativeExercise> = listOf(
        // Peito
        ex("supino_reto", "Supino reto com barra", CHEST, TRICEPS, SHOULDERS,
            description = "Deitado no banco reto, desça a barra até o meio do peito e empurre até estender os cotovelos."),
        ex("supino_reto_halteres", "Supino reto com halteres", CHEST, TRICEPS, SHOULDERS,
            description = "Deitado no banco reto, desça os halteres ao lado do peito e empurre aproximando-os no alto."),
        ex("supino_inclinado_barra", "Supino inclinado com barra", CHEST, SHOULDERS, TRICEPS,
            description = "No banco inclinado a 30–45°, desça a barra até a parte alta do peito e empurre."),
        ex("supino_inclinado_halteres", "Supino inclinado com halteres", CHEST, SHOULDERS, TRICEPS,
            description = "No banco inclinado a 30–45°, desça os halteres ao lado do peito e empurre para cima."),
        ex("supino_declinado", "Supino declinado com barra", CHEST, TRICEPS,
            description = "No banco declinado, desça a barra até a parte baixa do peito e empurre."),
        ex("crucifixo_halteres", "Crucifixo com halteres", CHEST,
            description = "Deitado, abra os braços com cotovelos levemente flexionados e feche em arco sobre o peito."),
        ex("crucifixo_maquina", "Crucifixo na máquina", CHEST,
            description = "Sentado no peck deck, aproxime os braços à frente do peito mantendo os cotovelos fixos."),
        ex("crossover", "Crossover na polia", CHEST,
            description = "Entre as polias altas, puxe os cabos em arco até as mãos se encontrarem à frente do quadril."),
        ex("flexao_bracos", "Flexão de braços", CHEST, TRICEPS, SHOULDERS,
            description = "Com o corpo alinhado, desça o peito até perto do chão e empurre. A carga é o peso adicional."),
        ex("paralelas", "Mergulho nas paralelas", CHEST, TRICEPS, SHOULDERS,
            description = "Nas barras paralelas, desça flexionando os cotovelos com o tronco inclinado e suba."),
        ex("pullover_halter", "Pullover com halter", CHEST, BACK,
            description = "Deitado, leve o halter atrás da cabeça com os braços quase estendidos e retorne sobre o peito."),

        // Costas
        ex("puxada_frontal", "Puxada frontal", BACK, BICEPS,
            description = "Sentado na polia alta, puxe a barra até a parte alta do peito aproximando as escápulas."),
        ex("barra_fixa", "Barra fixa", BACK, BICEPS,
            description = "Pendurado na barra, puxe até o queixo passar a barra. A carga é o peso adicional."),
        ex("remada_curvada", "Remada curvada com barra", BACK, BICEPS,
            description = "Com o tronco inclinado e coluna neutra, puxe a barra até o abdômen."),
        ex("remada_baixa", "Remada baixa na polia", BACK, BICEPS,
            description = "Sentado, puxe o triângulo até o abdômen mantendo o tronco estável."),
        ex("remada_unilateral", "Remada unilateral com halter", BACK, BICEPS,
            description = "Com um joelho e uma mão no banco, puxe o halter até o quadril."),
        ex("remada_cavalinho", "Remada cavalinho", BACK, BICEPS,
            description = "Com o tronco inclinado sobre a barra T, puxe a pegada até o peito."),
        ex("pulldown_bracos_estendidos", "Pulldown com braços estendidos na polia", BACK,
            description = "Em pé na polia alta, leve a barra até as coxas com os cotovelos quase estendidos."),
        ex("levantamento_terra", "Levantamento terra", BACK, GLUTES, HAMSTRINGS,
            description = "Com a barra junto às canelas e coluna neutra, levante estendendo quadril e joelhos juntos."),
        ex("hiperextensao_lombar", "Hiperextensão lombar no banco", BACK, GLUTES,
            description = "No banco romano, desça o tronco e suba até alinhá-lo com as pernas."),

        // Trapézio e ombros
        ex("encolhimento_halteres", "Encolhimento com halteres", TRAPS,
            description = "Em pé, eleve os ombros em direção às orelhas sem flexionar os cotovelos."),
        ex("remada_alta", "Remada alta com barra", SHOULDERS, TRAPS,
            description = "Em pé, puxe a barra junto ao corpo até a altura do peito com os cotovelos acima das mãos."),
        ex("desenvolvimento_halteres", "Desenvolvimento com halteres", SHOULDERS, TRICEPS,
            description = "Sentado, empurre os halteres da altura das orelhas até estender os cotovelos."),
        ex("desenvolvimento_militar", "Desenvolvimento militar com barra", SHOULDERS, TRICEPS,
            description = "Em pé, empurre a barra da altura das clavículas até acima da cabeça."),
        ex("elevacao_lateral", "Elevação lateral", SHOULDERS,
            description = "Em pé, eleve os halteres lateralmente até a altura dos ombros."),
        ex("elevacao_frontal", "Elevação frontal", SHOULDERS,
            description = "Em pé, eleve os halteres à frente do corpo até a altura dos ombros."),
        ex("crucifixo_inverso", "Crucifixo inverso", SHOULDERS, BACK,
            description = "Com o tronco inclinado ou na máquina, abra os braços para trás contraindo a parte posterior do ombro."),
        ex("face_pull", "Face pull na polia", SHOULDERS, TRAPS,
            description = "Com a corda na polia alta, puxe em direção ao rosto afastando as mãos no final."),

        // Bíceps e antebraço
        ex("rosca_direta", "Rosca direta com barra", BICEPS,
            description = "Em pé, flexione os cotovelos levando a barra até os ombros sem balançar o tronco."),
        ex("rosca_alternada", "Rosca alternada com halteres", BICEPS,
            description = "Em pé, flexione um braço de cada vez girando a palma para cima durante a subida."),
        ex("rosca_martelo", "Rosca martelo", BICEPS, FOREARMS,
            description = "Em pé, flexione os cotovelos com as palmas voltadas uma para a outra."),
        ex("rosca_scott", "Rosca Scott", BICEPS,
            description = "Com os braços apoiados no banco Scott, flexione os cotovelos sem tirar os braços do apoio."),
        ex("rosca_concentrada", "Rosca concentrada", BICEPS,
            description = "Sentado, com o cotovelo apoiado na coxa, flexione o braço levando o halter ao ombro."),
        ex("rosca_punho", "Rosca de punho", FOREARMS,
            description = "Com os antebraços apoiados, flexione apenas os punhos levantando a carga."),

        // Tríceps
        ex("triceps_polia", "Tríceps na polia", TRICEPS,
            description = "Em pé na polia alta, estenda os cotovelos mantendo os braços junto ao corpo."),
        ex("triceps_corda", "Tríceps corda na polia", TRICEPS,
            description = "Com a corda na polia alta, estenda os cotovelos afastando as mãos no final."),
        ex("triceps_frances", "Tríceps francês", TRICEPS,
            description = "Com o halter atrás da cabeça, estenda os cotovelos mantendo os braços apontados para cima."),
        ex("triceps_testa", "Tríceps testa com barra", TRICEPS,
            description = "Deitado, desça a barra em direção à testa flexionando só os cotovelos e estenda."),
        ex("mergulho_banco", "Mergulho no banco", TRICEPS, CHEST,
            description = "Com as mãos apoiadas num banco atrás do corpo, desça flexionando os cotovelos e suba."),

        // Quadríceps
        ex("agachamento_livre", "Agachamento livre", QUADS, GLUTES,
            description = "Com a barra nas costas, agache até as coxas ficarem paralelas ao chão e suba."),
        ex("agachamento_frontal", "Agachamento frontal", QUADS, GLUTES,
            description = "Com a barra apoiada na frente dos ombros, agache mantendo o tronco ereto."),
        ex("agachamento_bulgaro", "Agachamento búlgaro", QUADS, GLUTES,
            description = "Com o pé de trás apoiado num banco, desça o quadril flexionando a perna da frente."),
        ex("leg_press", "Leg press 45°", QUADS, GLUTES,
            description = "No leg press, desça a plataforma flexionando os joelhos e empurre sem travar no final."),
        ex("hack", "Agachamento no hack", QUADS, GLUTES,
            description = "Com as costas apoiadas no hack, agache e suba empurrando a plataforma com os pés."),
        ex("cadeira_extensora", "Cadeira extensora", QUADS,
            description = "Sentado na máquina, estenda os joelhos até as pernas ficarem retas."),
        ex("afundo_halteres", "Afundo com halteres", QUADS, GLUTES,
            description = "Dê um passo à frente e desça até o joelho de trás quase tocar o chão."),

        // Posteriores de coxa
        ex("mesa_flexora", "Mesa flexora", HAMSTRINGS,
            description = "Deitado de bruços na máquina, flexione os joelhos levando os calcanhares em direção ao glúteo."),
        ex("cadeira_flexora", "Cadeira flexora", HAMSTRINGS,
            description = "Sentado na máquina, flexione os joelhos empurrando o apoio para baixo."),
        ex("stiff", "Stiff", HAMSTRINGS, GLUTES,
            description = "Com os joelhos quase estendidos, desça a barra junto às pernas inclinando o quadril para trás."),
        ex("terra_romeno", "Levantamento terra romeno", HAMSTRINGS, GLUTES, BACK,
            description = "Com joelhos levemente flexionados, desça a barra até o meio das canelas e suba estendendo o quadril."),

        // Glúteos, adutores e abdutores
        ex("elevacao_pelvica", "Elevação pélvica", GLUTES, HAMSTRINGS,
            description = "Com as costas apoiadas no banco e a barra no quadril, eleve o quadril até alinhar o tronco."),
        ex("gluteo_maquina", "Glúteo na máquina", GLUTES,
            description = "Na máquina de coice, estenda o quadril empurrando o apoio para trás."),
        ex("cadeira_abdutora", "Cadeira abdutora", ABDUCTORS, GLUTES,
            description = "Sentado na máquina, afaste as pernas contra a resistência."),
        ex("cadeira_adutora", "Cadeira adutora", ADDUCTORS,
            description = "Sentado na máquina, aproxime as pernas contra a resistência."),

        // Panturrilhas
        ex("panturrilha_em_pe", "Panturrilha em pé", CALVES,
            description = "Em pé na máquina, eleve os calcanhares o máximo possível e desça alongando."),
        ex("panturrilha_sentado", "Panturrilha sentado", CALVES,
            description = "Sentado com o apoio sobre os joelhos, eleve os calcanhares e desça alongando."),
        ex("panturrilha_leg_press", "Panturrilha no leg press", CALVES,
            description = "No leg press, apoie a ponta dos pés na plataforma e empurre estendendo os tornozelos."),

        // Abdômen
        ex("abdominal_polia", "Abdominal na polia", ABS,
            description = "Ajoelhado na polia alta, flexione o tronco levando os cotovelos em direção aos joelhos."),
        ex("abdominal_crunch", "Abdominal crunch", ABS,
            description = "Deitado com os joelhos flexionados, eleve os ombros do chão contraindo o abdômen."),
        ex("elevacao_pernas", "Elevação de pernas", ABS,
            description = "Deitado ou pendurado, eleve as pernas estendidas até formar um ângulo de 90° com o tronco."),
    )
}
