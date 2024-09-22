package intellijautocompleteplugin;

import com.intellij.codeInsight.inline.completion.InlineCompletionRequest;
import intellijautocompleteplugin.cache.CacheEntry;
import intellijautocompleteplugin.cache.CompletionCache;
import intellijautocompleteplugin.cache.LRUCache;
import intellijautocompleteplugin.completion.CompletionService;
import intellijautocompleteplugin.ollama.OllamaClient;
import io.github.ollama4j.exceptions.OllamaBaseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import com.intellij.openapi.editor.Document;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

class CompletionServiceTest {

    @Mock
    private OllamaClient ollamaClient;
    @Mock
    private InlineCompletionRequest request;
    @Mock
    private Document document;
    private CompletionService completionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        completionService = new CompletionService(ollamaClient);
        when(request.getDocument()).thenReturn(document);
    }

    @Test
    void testGetCompletion_CacheHit() {
        // ARRANGE
        String codeBeforeCaret = "System.out";
        String currentWord = "System.out";
        String cachedValue = "System.out.println();";
        CacheEntry cacheEntry = new CacheEntry(cachedValue, System.currentTimeMillis());

        try (MockedStatic<CompletionCache> cacheMock = mockStatic(CompletionCache.class)) {
            cacheMock.when(CompletionCache::getKeySet).thenReturn(new HashSet<>(List.of(currentWord)));
            cacheMock.when(() -> CompletionCache.get(currentWord)).thenReturn(Optional.of(cacheEntry));

            when(document.getText()).thenReturn(codeBeforeCaret);
            when(request.getStartOffset()).thenReturn(codeBeforeCaret.length() - 1);

            // ACT
            List<String> suggestions = completionService.getCompletion(request);

            // ASSERT
            assertEquals(1, suggestions.size());
            assertEquals(".println();", suggestions.get(0));
        }
    }

    @Test
    void testGetCompletion_CacheMiss() throws Exception {
        // ARRANGE
        String codeBeforeCaret = "System.";
        String currentWord = "System.";
        String aiCompletion = "System.out.println();";

        try (MockedStatic<CompletionCache> cacheMock = mockStatic(CompletionCache.class)) {
            cacheMock.when(CompletionCache::getKeySet).thenReturn(new HashSet<>());
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
    }

    @Test
    void testGetCompletion_CacheExpired() throws Exception {
        // ARRANGE
        String codeBeforeCaret = "System.out";
        String currentWord = "System.out";
        String cachedValue = "System.out.println();";
        CacheEntry cacheEntry = new CacheEntry(cachedValue, System.currentTimeMillis() - CompletionCache.CACHE_TTL - 1);

        try (MockedStatic<CompletionCache> cacheMock = mockStatic(CompletionCache.class)) {
            cacheMock.when(CompletionCache::getKeySet).thenReturn(new HashSet<>(List.of(currentWord)));
            cacheMock.when(() -> CompletionCache.get(currentWord)).thenReturn(Optional.of(cacheEntry));
            cacheMock.when(() -> CompletionCache.isExpired(cacheEntry)).thenReturn(true);

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


}

