package intellijautocompleteplugin;

import com.intellij.codeInsight.inline.completion.InlineCompletionRequest;
import intellijautocompleteplugin.cache.CacheEntry;
import intellijautocompleteplugin.cache.LRUCache;
import intellijautocompleteplugin.completion.CompletionService;
import intellijautocompleteplugin.ollama.OllamaClient;
import io.github.ollama4j.exceptions.OllamaBaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.intellij.openapi.editor.Document;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CompletionServiceTest {

    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private LRUCache<String, CacheEntry> completionCache;
    @Mock
    private InlineCompletionRequest request;
    @Mock
    private Document document;
    private CompletionService completionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        completionService = new CompletionService(ollamaClient, completionCache);
        when(request.getDocument()).thenReturn(document);
    }

    @Test
    void testGetCompletion_CacheHit() {
        // ARRANGE
        String codeBeforeCaret = "System.out";
        String currentWord = "System.out";
        String cachedValue = "System.out.println();";
        CacheEntry cacheEntry = new CacheEntry(cachedValue, System.currentTimeMillis());

        when(completionCache.keySet()).thenReturn(new HashSet<>(List.of(currentWord)));
        when(completionCache.get(currentWord)).thenReturn(cacheEntry);

        when(document.getText()).thenReturn(codeBeforeCaret);
        when(request.getStartOffset()).thenReturn(codeBeforeCaret.length() - 1);

        // ACT
        List<String> suggestions = completionService.getCompletion(request);

        // ASSERT
        assertEquals(1, suggestions.size());
        assertEquals(".println();", suggestions.get(0));
    }

    @Test
    void testGetCompletion_CacheMiss() throws Exception {
        // ARRANGE
        String codeBeforeCaret = "System.";
        String currentWord = "System.";
        String aiCompletion = "System.out.println();";

        when(completionCache.keySet()).thenReturn(new HashSet<>());
        when(ollamaClient.sendQuery(anyString())).thenReturn(aiCompletion);
        when(document.getText()).thenReturn(codeBeforeCaret);
        when(request.getStartOffset()).thenReturn(codeBeforeCaret.length() - 1);

        // ACT
        List<String> suggestions = completionService.getCompletion(request);

        // ASSERT
        verify(ollamaClient, times(1)).sendQuery(anyString());

        assertEquals(1, suggestions.size());
        assertEquals("out.println();", suggestions.get(0));
    }

    @Test
    void testGetCompletion_CacheExpired() throws Exception {
        // ARRANGE
        String codeBeforeCaret = "System.out";
        String currentWord = "System.out";
        String cachedValue = "System.out.println();";
        CacheEntry cacheEntry = new CacheEntry(cachedValue, System.currentTimeMillis() - completionService.getCacheTTL() - 1);

        when(completionCache.keySet()).thenReturn(new HashSet<>(List.of(currentWord)));
        when(completionCache.get(currentWord)).thenReturn(cacheEntry);

        String aiCompletion = "out.printStackTrace();";
        when(ollamaClient.sendQuery(anyString())).thenReturn(aiCompletion);

        when(document.getText()).thenReturn(codeBeforeCaret);
        when(request.getStartOffset()).thenReturn(codeBeforeCaret.length() - 1);

        // ACT
        List<String> suggestions = completionService.getCompletion(request);

        // ASSERT
        verify(ollamaClient, times(1)).sendQuery(anyString());

        assertEquals(1, suggestions.size());
        assertEquals(".printStackTrace();", suggestions.get(0));
    }

}

