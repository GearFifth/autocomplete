package intellijautocompleteplugin

import com.intellij.codeInsight.inline.completion.*
import com.intellij.openapi.progress.coroutineToIndicator
import intellijautocompleteplugin.completion.CompletionService
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import com.intellij.codeInsight.inline.completion.elements.InlineCompletionGrayTextElement

val DEBOUNCE_DELAY = 1000.milliseconds

@Suppress("UnstableApiUsage")
public class SuggestionsProvider : DebouncedInlineCompletionProvider() {
    override val id = InlineCompletionProviderID(this::class.java.name)

    override suspend fun getSuggestionDebounced(request: InlineCompletionRequest): InlineCompletionSuggestion {

        val completionService = CompletionService()

        val elements = coroutineToIndicator {
            completionService.getCompletion(request)
        } ?: return InlineCompletionSuggestion.empty()

        return InlineCompletionSuggestion.withFlow {
            val flowElements: Flow<String> = elements.asFlow()

            flowElements.collect { suggestion ->
                val inlineElement = InlineCompletionGrayTextElement(suggestion)
                emit(inlineElement)
            }
        }
    }
    override suspend fun getDebounceDelay(request: InlineCompletionRequest): Duration = DEBOUNCE_DELAY

    override fun isEnabled(event: InlineCompletionEvent): Boolean = true

}

