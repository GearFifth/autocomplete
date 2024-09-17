package intellijautocompleteplugin

import com.gitlab.plugin.codesuggestions.render.UpdatableInlineCompletionGrayElement
import com.intellij.codeInsight.inline.completion.*
import com.intellij.openapi.progress.coroutineToIndicator
import intellijautocompleteplugin.completion.CompletionService
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val DEBOUNCE_DELAY = 1000.milliseconds

@Suppress("UnstableApiUsage")
public class SuggestionsProvider : DebouncedInlineCompletionProvider() {
    override val id = InlineCompletionProviderID(this::class.java.name)

    override suspend fun getSuggestionDebounced(request: InlineCompletionRequest): InlineCompletionSuggestion {

        val elements = coroutineToIndicator {
            CompletionService.getCompletion(request)
        } ?: return InlineCompletionSuggestion.empty()

        return InlineCompletionSuggestion.withFlow {
            val flowElements: Flow<String> = elements.asFlow()

            flowElements.collect { suggestion ->
                val inlineElement = UpdatableInlineCompletionGrayElement(suggestion, request.editor)
                emit(inlineElement)
            }
        }
    }
    override suspend fun getDebounceDelay(request: InlineCompletionRequest): Duration = DEBOUNCE_DELAY

    override fun isEnabled(event: InlineCompletionEvent): Boolean = true

}

