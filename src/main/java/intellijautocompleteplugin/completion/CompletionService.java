package intellijautocompleteplugin.completion;

import com.intellij.codeInsight.inline.completion.InlineCompletionRequest;
import intellijautocompleteplugin.cache.CompletionCache;
import intellijautocompleteplugin.ollama.OllamaClient;
import intellijautocompleteplugin.cache.CacheEntry;
import intellijautocompleteplugin.cache.LRUCache;
import io.github.ollama4j.exceptions.OllamaBaseException;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class CompletionService {

    private final OllamaClient ollamaClient;
    private final int WINDOW = 100;

    public CompletionService() {
        this.ollamaClient = new OllamaClient();
    }

    public CompletionService(OllamaClient ollamaClient) {
        this.ollamaClient = ollamaClient;
    }

    /**
     * Fetches completion suggestions based on the current inline request.
     *
     * @param request The request containing the current state of the document and caret.
     * @return A list of completion suggestions.
     */
    public List<String> getCompletion(@NotNull InlineCompletionRequest request) {
        List<String> suggestions = new ArrayList<>();
        String codeBeforeCaret = getCodeBeforeCaret(request);
        char lastChar = codeBeforeCaret.charAt(codeBeforeCaret.length()-1);
        String currentWord = getLastSubstringWithoutWhitespaces(codeBeforeCaret);

//        long startTime = System.nanoTime(); // TIMER

        Optional<String> bestMatch = Optional.empty();
        if(lastChar != '\n'){
            bestMatch = findMatch(currentWord);
        }

        if (bestMatch.isPresent()) {
            CacheEntry cacheEntry = CompletionCache.get(bestMatch.get()).orElse(null);

            if (cacheEntry != null && !CompletionCache.isExpired(cacheEntry)) {
                String suggestion = formatSuggestion(cacheEntry.getValue(), currentWord);
                suggestions.add(suggestion);
                return suggestions;
            }
        }
        fetchCompletionFromAI(codeBeforeCaret, currentWord, suggestions);

//        long endTime = System.nanoTime();
//        long durationInMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
//        System.out.println("Time taken for completion: " + durationInMillis + " ms");

        return suggestions;

    }

//    private String formatSuggestion(String suggestion, String currentWord){
//        if(suggestion.startsWith(currentWord)){
//            return suggestion.substring(currentWord.length());
//        }
//        return suggestion;
//    }

    private String formatSuggestion(String suggestion, String prefix) {
        for (int i = 0; i < prefix.length(); i++) {
            String trimmedPrefix = prefix.substring(i);
            if (suggestion.startsWith(trimmedPrefix)) {
                return suggestion.substring(trimmedPrefix.length());
            }
        }
        return suggestion;
    }

    private String formQuery(String codeBeforeCaret) {
        return String.format(
                """
                Suggest the inline completion at the caret position. Please return only the suggestion without any additional text or explanation.
                Code before caret: %s
                """,
                codeBeforeCaret
        );
    }

    private String getCodeBeforeCaret(@NotNull InlineCompletionRequest request) {
        com.intellij.openapi.editor.Document document = request.getDocument();
        int caretOffset = request.getStartOffset() + 1;
        String code = document.getText();
        return code.substring(Math.max(caretOffset - WINDOW, 0), caretOffset);
//        return code.substring(0, caretOffset);
    }


    /**
     * Searches for the best matching cached completion for the current word.
     *
     * @param currentWord The word to find a match for.
     * @return An optional containing the best match from the cache.
     */
    private Optional<String> findMatch(String currentWord) {
        List<String> cachedKeys = new ArrayList<>(CompletionCache.getKeySet());

        for (String key : cachedKeys){
            int minLen = Math.min(currentWord.length(), key.length());
            boolean isMatch = true;

            for(int i = 0; i < minLen; i++){
                if(currentWord.charAt(i) != key.charAt(i)) {
                    isMatch = false;
                    break;
                }
            }

            if(isMatch){
                return Optional.of(key);
            }
        }

        return Optional.empty();
    }

    /**
     * Fetches a completion suggestion from the AI model and adds it to the list of suggestions.
     * The result is also cached for future use.
     *
     * @param codeBeforeCaret The code before the caret position.
     * @param currentWord The current word being completed.
     * @param suggestions The list of suggestions to be returned.
     */
    private void fetchCompletionFromAI(String codeBeforeCaret, String currentWord, List<String> suggestions) {
        try {
            String query = formQuery(codeBeforeCaret);
            System.out.println("Query: " + query);
            String aiCompletion = ollamaClient.sendQuery(query);
            System.out.println("AI Completion: " + aiCompletion);
            String suggestion = formatSuggestion(aiCompletion, currentWord);
            suggestions.add(suggestion);

            CacheEntry cacheEntry = new CacheEntry(currentWord + suggestion, System.currentTimeMillis());
            CompletionCache.put(currentWord, cacheEntry);
        } catch (InterruptedException | OllamaBaseException e) {
            e.printStackTrace();
        }
    }

    public String getLastSubstringWithoutWhitespaces(String input) {
        input = input.trim();

        String[] parts = input.split("\\s+");

        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return "";
    }

}
