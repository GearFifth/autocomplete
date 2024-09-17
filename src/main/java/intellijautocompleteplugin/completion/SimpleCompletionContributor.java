package intellijautocompleteplugin.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class SimpleCompletionContributor extends CompletionContributor {
    public SimpleCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext context, @NotNull CompletionResultSet result) {
                // Add your custom completion items here
                result.addElement(LookupElementBuilder.create("myCompletionItem1"));
                result.addElement(LookupElementBuilder.create("myCompletionItem2"));

                result.stopHere();
            }
        });
    }
}