package intellijautocompleteplugin.toolWindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import intellijautocompleteplugin.ollama.OllamaClient;
import io.github.ollama4j.exceptions.OllamaBaseException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class SampleToolWindowFactory implements ToolWindowFactory, DumbAware {
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        OllamaToolWindowContent toolWindowContent = new OllamaToolWindowContent(toolWindow);
        Content content = ContentFactory.getInstance().createContent(toolWindowContent.getContentPanel(), "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private static class OllamaToolWindowContent {

        private final JPanel contentPanel = new JPanel();
        private final JTextArea queryInput = new JTextArea(5, 40);
        private final JTextArea responseOutput = new JTextArea(10, 40);
        private final OllamaClient ollamaClient = new OllamaClient();

        public OllamaToolWindowContent(ToolWindow toolWindow) {
            contentPanel.setLayout(new BorderLayout(10, 10));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            contentPanel.add(createQueryPanel(), BorderLayout.PAGE_START);
            contentPanel.add(createControlsPanel(toolWindow), BorderLayout.PAGE_END);
        }

        @NotNull
        private JPanel createQueryPanel() {
            JPanel queryPanel = new JPanel();
            queryPanel.setLayout(new BoxLayout(queryPanel, BoxLayout.Y_AXIS));
            queryPanel.add(new JLabel("Enter Query:"));
            queryInput.setLineWrap(true);
            queryInput.setWrapStyleWord(true);
            queryPanel.add(new JScrollPane(queryInput));
            queryPanel.add(new JLabel("Response:"));
            responseOutput.setEditable(false);
            queryPanel.add(new JScrollPane(responseOutput));
            return queryPanel;
        }

        @NotNull
        private JPanel createControlsPanel(ToolWindow toolWindow) {
            JPanel controlsPanel = new JPanel();
            JButton sendQueryButton = new JButton("Send Query");
            sendQueryButton.addActionListener(e -> sendQueryToOllama());
            controlsPanel.add(sendQueryButton);
            JButton hideToolWindowButton = new JButton("Hide");
            hideToolWindowButton.addActionListener(e -> toolWindow.hide(null));
            controlsPanel.add(hideToolWindowButton);
            return controlsPanel;
        }

        private void sendQueryToOllama() {
            String query = queryInput.getText();
            if (!query.isEmpty()) {
                try {
                    String response = ollamaClient.sendQuery(query);
                    responseOutput.setText(response);
                } catch (OllamaBaseException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                responseOutput.setText("Please enter a query.");
            }
        }

        public JPanel getContentPanel() {
            return contentPanel;
        }
    }
}
