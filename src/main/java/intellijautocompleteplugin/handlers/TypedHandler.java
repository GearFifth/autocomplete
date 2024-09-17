package intellijautocompleteplugin.handlers;

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class TypedHandler extends TypedHandlerDelegate {
    @Override
    public Result charTyped(char c, Project project, Editor editor, PsiFile file) {
        Document document = editor.getDocument();
        int caretOffset = editor.getCaretModel().getOffset();

        String code = document.getText();

        String codeBeforeCaret = code.substring(0, caretOffset - 1);
        String codeAfterCaret = code.substring(caretOffset);

        return Result.CONTINUE;
    }
}
