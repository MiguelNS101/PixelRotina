package com.example.data

import com.example.ui.StreakMedalSummary
import com.example.ui.WeeklyReportSummary
import com.example.ui.components.PixelSpriteType
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AiEntryAnalysisResult(
    val responseText: String,
    val suggestedTags: List<String> = emptyList(),
    val suggestedRoutineJson: String = ""
)

/**
 * Encapsulated optional Gemini SDK helper for the Journal module.
 * Instantiates GenerativeModel strictly on-demand and catches all exceptions into Result<T>
 * so offline operation or invalid/missing API keys never crash the app.
 */
class JournalAiAssistant(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL_NAME
) {
    private fun createModel(): GenerativeModel {
        val cleanModel = modelName.trim().ifEmpty { DEFAULT_MODEL_NAME }
        return GenerativeModel(
            modelName = cleanModel,
            apiKey = apiKey.trim()
        )
    }

    suspend fun analyzeJournalEntry(
        entry: JournalEntryEntity,
        mode: AiReactionMode,
        recentRoutinesTitles: List<String>
    ): Result<AiEntryAnalysisResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Chave da API Gemini não configurada."))
        }
        if (mode == AiReactionMode.NONE) {
            return@withContext Result.failure(IllegalArgumentException("Modo de análise inválido."))
        }

        runCatching {
            val model = createModel()
            val prompt = buildEntryPrompt(entry, mode, recentRoutinesTitles)
            val response = model.generateContent(prompt)
            val rawText = response.text?.trim().orEmpty()
            if (rawText.isBlank()) {
                error("O modelo retornou uma resposta vazia.")
            }

            when (mode) {
                AiReactionMode.ACTIONABLE_STEPS -> extractActionableResult(rawText)
                AiReactionMode.VALIDATE_SUMMARY -> extractSummaryResult(rawText)
                AiReactionMode.SOCRATIC_QUESTIONS -> AiEntryAnalysisResult(responseText = rawText)
                AiReactionMode.NONE -> AiEntryAnalysisResult(responseText = rawText)
            }
        }
    }

    suspend fun generateWeeklyPatchNotes(
        recentEntries: List<JournalEntryEntity>,
        weeklyReport: WeeklyReportSummary,
        streakSummary: StreakMedalSummary
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("Chave da API Gemini não configurada."))
        }

        runCatching {
            val model = createModel()
            val prompt = buildWeeklyPatchNotesPrompt(recentEntries, weeklyReport, streakSummary)
            val response = model.generateContent(prompt)
            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                error("Não foi possível gerar os Patch Notes da semana.")
            }
            text
        }
    }

    private fun buildEntryPrompt(
        entry: JournalEntryEntity,
        mode: AiReactionMode,
        recentRoutinesTitles: List<String>
    ): String {
        val moodDesc = entry.moodScore?.let { "$it/5" } ?: "não informado"
        val energyDesc = entry.energyScore?.let { "$it/5" } ?: "não informada"
        val tagsDesc = entry.contextTags.ifEmpty { listOf("nenhuma") }.joinToString(", ")
        val routinesDesc = recentRoutinesTitles.take(8).ifEmpty { listOf("nenhuma ativa") }.joinToString(", ")

        val contextBlock = """
            Contexto do usuário no app PixelRotina (pt-BR):
            - Humor registrado: $moodDesc | Energia registrada: $energyDesc
            - Tags de contexto: $tagsDesc
            - Snapshot automático do dia: ${entry.routineCompletionPercentSnapshot}% das rotinas concluídas, Medalha: ${entry.medalSnapshot.titlePt}, Ciclos Pomodoro: ${entry.pomodoroCyclesSnapshot}
            - Rotinas atuais: $routinesDesc
            - Texto do diário (${entry.entryType.labelPt}): "${entry.content.ifBlank { "(sem texto, apenas check-in de humor/energia)" }}"
        """.trimIndent()

        return when (mode) {
            AiReactionMode.VALIDATE_SUMMARY -> """
                $contextBlock

                Responda em português do Brasil de forma acolhedora, curta e direta (máximo 3 linhas):
                1) Valide o momento do usuário e faça um breve resumo empático relacionando humor/energia e a rotina.
                2) Na última linha, sugira até 3 tags curtas no formato: TAGS: TAG1, TAG2, TAG3
                Sem markdown pesado ou listas longas.
            """.trimIndent()

            AiReactionMode.SOCRATIC_QUESTIONS -> """
                $contextBlock

                Responda em português do Brasil com EXATAMENTE 2 perguntas curtas, gentis e diretas (numeradas 1. e 2.) para destravar a reflexão do usuário sobre o que está vivendo hoje.
                Não adicione introdução nem conclusão.
            """.trimIndent()

            AiReactionMode.ACTIONABLE_STEPS -> {
                val categories = RoutineCategory.entries.joinToString(", ") { it.name }
                val sprites = PixelSpriteType.entries.joinToString(", ") { it.name }
                """
                    $contextBlock

                    Responda em português do Brasil:
                    1) Escreva 2 a 3 linhas práticas sugerindo um ajuste simples de rotina sem pressão.
                    2) Em seguida, forneça EXATAMENTE um bloco JSON delimitado entre $ROUTINE_JSON_START e $ROUTINE_JSON_END sugerindo 1 rotina opcional que ajude o usuário, usando exatamente estas chaves:
                    $ROUTINE_JSON_START
                    {
                      "title": "Nome curto da rotina",
                      "description": "Descrição curta e gentil",
                      "categoryName": "UMA_DESTAS: $categories",
                      "spriteName": "UM_DESTES: $sprites",
                      "frequencyTypeName": "SPECIFIC_TIME ou INTERVAL",
                      "intervalMinutes": 60,
                      "targetCountPerDay": 1
                    }
                    $ROUTINE_JSON_END
                """.trimIndent()
            }

            AiReactionMode.NONE -> contextBlock
        }
    }

    private fun buildWeeklyPatchNotesPrompt(
        recentEntries: List<JournalEntryEntity>,
        weeklyReport: WeeklyReportSummary,
        streakSummary: StreakMedalSummary
    ): String {
        val entriesSummary = if (recentEntries.isEmpty()) {
            "Nenhuma nota de diário registrada nos últimos 7 dias."
        } else {
            recentEntries.take(14).joinToString("\n") { e ->
                val mood = e.moodScore?.let { "H:$it" } ?: "H:-"
                val energy = e.energyScore?.let { "E:$it" } ?: "E:-"
                val tags = e.contextTags.joinToString(",")
                val snippet = e.content.replace("\n", " ").take(120)
                "- [${e.dateKey}] ($mood/$energy | Rotina ${e.routineCompletionPercentSnapshot}% | Tags: $tags) $snippet"
            }
        }

        return """
            Você é o assistente gentil do app PixelRotina (estética gamer/pixel art retrô).
            Gere um relatório curto em português do Brasil no estilo "PATCH NOTES DA SEMANA".

            Dados dos últimos 7 dias:
            - Total de check-ins concluídos: ${weeklyReport.totalCompletedWeek}
            - Melhor período do dia: ${weeklyReport.bestPeriodOfDay}
            - Sequência atual: ${streakSummary.currentStreakDays} dias (Recorde: ${streakSummary.bestStreakDays} dias)
            - Registros do diário:
            $entriesSummary

            Responda EXATAMENTE com estes 3 blocos curtos (2 frases/itens por bloco, tom encorajador e direto, sem excesso de markdown):
            ▲ Buffs (O que funcionou)
            ▼ Nerfs / Drenos (Padrões de queda)
            ★ Próxima Quest (1 ajuste prático)
        """.trimIndent()
    }

    companion object {
        const val DEFAULT_MODEL_NAME = "gemini-2.5-flash"
        const val PRO_MODEL_NAME = "gemini-2.5-pro"
        const val ROUTINE_JSON_START = "<<<ROUTINE_JSON>>>"
        const val ROUTINE_JSON_END = "<<<END_ROUTINE_JSON>>>"

        fun extractSummaryResult(rawText: String): AiEntryAnalysisResult {
            val lines = rawText.lines()
            val tagsLine = lines.lastOrNull { it.trim().startsWith("TAGS:", ignoreCase = true) }
            val parsedTags = tagsLine
                ?.substringAfter(":", "")
                ?.split(",")
                ?.map { it.trim().removePrefix("#").uppercase() }
                ?.filter { it.isNotEmpty() }
                ?.take(3)
                .orEmpty()
            return AiEntryAnalysisResult(
                responseText = rawText.trim(),
                suggestedTags = parsedTags
            )
        }

        fun extractActionableResult(rawText: String): AiEntryAnalysisResult {
            val startIdx = rawText.indexOf(ROUTINE_JSON_START)
            val endIdx = rawText.indexOf(ROUTINE_JSON_END)
            if (startIdx >= 0 && endIdx > startIdx) {
                val jsonCandidate = rawText
                    .substring(startIdx + ROUTINE_JSON_START.length, endIdx)
                    .trim()
                val cleanedText = (rawText.substring(0, startIdx) + rawText.substring(endIdx + ROUTINE_JSON_END.length))
                    .trim()
                val validJson = if (parseSuggestedRoutineJsonToEntity(jsonCandidate) != null) jsonCandidate else ""
                return AiEntryAnalysisResult(
                    responseText = cleanedText.ifBlank { "Sugestão de rotina pronta para revisão!" },
                    suggestedRoutineJson = validJson
                )
            }

            // Fallback: try fenced ```json ... ``` block
            val regex = Regex("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", RegexOption.IGNORE_CASE)
            val match = regex.find(rawText)
            if (match != null) {
                val jsonCandidate = match.groupValues[1].trim()
                val cleanedText = rawText.replace(match.value, "").trim()
                val validJson = if (parseSuggestedRoutineJsonToEntity(jsonCandidate) != null) jsonCandidate else ""
                return AiEntryAnalysisResult(
                    responseText = cleanedText.ifBlank { "Sugestão de rotina pronta para revisão!" },
                    suggestedRoutineJson = validJson
                )
            }

            return AiEntryAnalysisResult(responseText = rawText.trim())
        }

        /**
         * Converts a structured routine JSON string produced by Gemini into a draft RoutineEntity
         * ready to populate RoutineEditorDialog (id = 0L).
         */
        fun parseSuggestedRoutineJsonToEntity(suggestedJson: String): RoutineEntity? {
            if (suggestedJson.isBlank()) return null
            return try {
                val obj = JSONObject(suggestedJson.trim())
                val rawTitle = obj.optString("title", "").trim()
                if (rawTitle.isBlank()) return null

                val rawDescription = obj.optString("description", "").trim()
                val rawCategory = obj.optString("categoryName", "").trim().uppercase()
                val category = RoutineCategory.entries.firstOrNull { it.name == rawCategory }
                    ?: RoutineCategory.CORPO_MENTE

                val rawSprite = obj.optString("spriteName", "").trim().uppercase()
                val sprite = PixelSpriteType.entries.firstOrNull { it.name == rawSprite }
                    ?: category.defaultSprite

                val rawFreq = obj.optString("frequencyTypeName", "").trim().uppercase()
                val frequency = FrequencyType.entries.firstOrNull { it.name == rawFreq }
                    ?: FrequencyType.SPECIFIC_TIME

                val intervalMinutes = obj.optInt("intervalMinutes", 60).coerceIn(15, 240)
                val targetCount = obj.optInt(
                    "targetCountPerDay",
                    if (frequency == FrequencyType.INTERVAL) 4 else 1
                ).coerceIn(1, 12)
                val hour = obj.optInt("hour", 9).coerceIn(0, 23)
                val minute = obj.optInt("minute", 0).coerceIn(0, 59)

                RoutineEntity(
                    id = 0L,
                    title = rawTitle.take(60),
                    description = rawDescription.take(160),
                    categoryName = category.name,
                    spriteName = sprite.name,
                    alertModeName = AlertMode.QUICK_OK_NOTIFICATION.name,
                    frequencyTypeName = frequency.name,
                    hour = hour,
                    minute = minute,
                    intervalMinutes = intervalMinutes,
                    targetCountPerDay = targetCount,
                    daysOfWeekCsv = "1,2,3,4,5,6,7",
                    isEnabled = true
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
