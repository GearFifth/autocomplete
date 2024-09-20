package intellijautocompleteplugin.completion;

import com.intellij.codeInsight.inline.completion.InlineCompletionRequest;
import com.intuit.fuzzymatcher.component.MatchService;
import com.intuit.fuzzymatcher.domain.Document;
import com.intuit.fuzzymatcher.domain.Element;
import com.intuit.fuzzymatcher.domain.ElementType;
import com.intuit.fuzzymatcher.domain.Match;
import intellijautocompleteplugin.ollama.OllamaClient;
import intellijautocompleteplugin.cache.LRUCache;
import io.github.ollama4j.exceptions.OllamaBaseException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//Implementation with fuzzy matching library
public class CompletionServiceFuzzy {

    private static final OllamaClient ollamaClient = new OllamaClient();
    private static final MatchService matchService = new MatchService();
    private static final int CACHE_CAPACITY = 200;
    private static final LRUCache<String, String> completionCache = new LRUCache<>(CACHE_CAPACITY);
    private static final double MATCH_THRESHOLD = 0.8;
    private static final int WINDOW = 100;

    public static List<String> getCompletion(@NotNull InlineCompletionRequest request) {
        List<String> suggestions = new ArrayList<>();
        String codeBeforeCaret = getCodeBeforeCaret(request);
        char lastChar = codeBeforeCaret.charAt(codeBeforeCaret.length()-1);
        long startTime = System.nanoTime();

        Optional<Match<Document>> bestMatch = Optional.empty();
        if(lastChar != '\n'){
            bestMatch = findBestFuzzyMatch(getLastSubstringWithoutWhitespaces(codeBeforeCaret));
        }

        if(bestMatch.isPresent()){
            String key = bestMatch.get().getMatchedWith().getKey();
            suggestions.add(completionCache.get(key));
        } else {
            fetchCompletionFromAI(codeBeforeCaret, suggestions);
        }

        long endTime = System.nanoTime();
        long durationInMillis = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        System.out.println("Time taken for completion: " + durationInMillis + " ms");

        filterSuggestions(suggestions, codeBeforeCaret);
        return suggestions;
    }

    private static void filterSuggestions(List<String> suggestions, String codeBeforeCaret){
        String firstSuggestion = suggestions.remove(0);
        String modifiedSuggestion = removeCommonPrefix(getLastSubstringWithoutWhitespaces(codeBeforeCaret), firstSuggestion);
        suggestions.add(0, modifiedSuggestion);
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

    private static Optional<Match<Document>> findBestFuzzyMatch(String codeBeforeCaret) {
        List<Document> cachedDocuments = completionCache.entrySet().stream()
                .map(entry -> createFuzzyDocument(entry.getKey(), entry.getKey() + entry.getValue()))
                .collect(Collectors.toList());

        Document currentDocument = createFuzzyDocument(codeBeforeCaret, codeBeforeCaret);

        Map<Document, List<Match<Document>>> matches = matchService.applyMatch(currentDocument, cachedDocuments);

        return matches.values().stream()
                .flatMap(List::stream)
                .filter(match -> match.getScore().getResult() >= MATCH_THRESHOLD)
                .max(Comparator.comparingDouble(match -> match.getScore().getResult()));
    }

    private static void fetchCompletionFromAI(String codeBeforeCaret, List<String> suggestions) {
        try {
            String query = formQuery(codeBeforeCaret);
            System.out.println("Query: " + query);
            String aiCompletion = ollamaClient.sendQuery(query);
            System.out.println("AI Completion: " + aiCompletion);
            String lastSubstring = getLastSubstringWithoutWhitespaces(codeBeforeCaret);
            String trimmedCompletion = removeCommonPrefix(lastSubstring, aiCompletion);
            suggestions.add(trimmedCompletion);
            completionCache.put(lastSubstring, trimmedCompletion);
        } catch (InterruptedException | OllamaBaseException e) {
            e.printStackTrace();
        }
    }

    private static Document createFuzzyDocument(String id, String content) {
        return new Document.Builder(id)
                .addElement(new Element.Builder<String>().setValue(content).setType(ElementType.TEXT).createElement())
                .createDocument();
    }

    public static String removeCommonPrefix(String str1, String str2) {
        int minLength = Math.min(str1.length(), str2.length());
        int i = 0;

        while (i < minLength && str1.charAt(i) == str2.charAt(i)) {
            i++;
        }

        return str2.substring(i);
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
