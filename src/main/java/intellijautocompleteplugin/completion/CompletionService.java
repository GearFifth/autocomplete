package intellijautocompleteplugin.completion;

import com.intellij.codeInsight.inline.completion.InlineCompletionRequest;
import intellijautocompleteplugin.ollama.OllamaClient;
import intellijautocompleteplugin.utils.CacheEntry;
import intellijautocompleteplugin.utils.LRUCache;
import io.github.ollama4j.exceptions.OllamaBaseException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CompletionService {

    private static final OllamaClient ollamaClient = new OllamaClient();
    private static final int CACHE_CAPACITY = 20;

    private static final int CACHE_TTL = 10 * 60 * 1000; // 10 minutes
    private static final LRUCache<String, CacheEntry> completionCache = new LRUCache<>(CACHE_CAPACITY);
    private static final int WINDOW = 100;

    public static List<String> getCompletion(@NotNull InlineCompletionRequest request) {
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
            CacheEntry cacheEntry = completionCache.get(bestMatch.get());

            boolean isExpired = System.currentTimeMillis() - cacheEntry.getTimestamp() > CACHE_TTL;
            if (!isExpired) {
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

    private static String formatSuggestion(String suggestion, String currentWord){
        if(suggestion.startsWith(currentWord)){
            return suggestion.substring(currentWord.length());
        }
        return suggestion;
    }

    private static String formQuery(String codeBeforeCaret) {
        return String.format(
                """
                Suggest the inline completion at the caret position. Please return only the suggestion without any additional text or explanation.
                Code before caret: %s
                """,
                codeBeforeCaret
        );
    }

    private static String getCodeBeforeCaret(@NotNull InlineCompletionRequest request) {
        com.intellij.openapi.editor.Document document = request.getDocument();
        int caretOffset = request.getStartOffset() + 1;
        String code = document.getText();
        return code.substring(Math.max(caretOffset - WINDOW, 0), caretOffset);
//        return code.substring(0, caretOffset);
    }


    private static Optional<String> findMatch(String currentWord) {
        List<String> cachedKeys = new ArrayList<>(completionCache.keySet());

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

    private static void fetchCompletionFromAI(String codeBeforeCaret, String currentWord, List<String> suggestions) {
        try {
            String query = formQuery(codeBeforeCaret);
            System.out.println("Query: " + query);
            String aiCompletion = ollamaClient.sendQuery(query);
            System.out.println("AI Completion: " + aiCompletion);
            String suggestion = formatSuggestion(aiCompletion, currentWord);
            suggestions.add(suggestion);

            CacheEntry cacheEntry = new CacheEntry(currentWord + suggestion, System.currentTimeMillis());
            completionCache.put(currentWord, cacheEntry);
        } catch (InterruptedException | OllamaBaseException e) {
            e.printStackTrace();
        }
    }

    public static String getLastSubstringWithoutWhitespaces(String input) {
        input = input.trim();

        String[] parts = input.split("\\s+");

        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return "";
    }


}
