package intellijautocompleteplugin.ollama;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.OllamaAsyncResultStreamer;
import io.github.ollama4j.types.OllamaModelType;
import io.github.ollama4j.utils.OptionsBuilder;

public class OllamaClient {
    private final OllamaAPI ollamaAPI;
    private final String MODEL = OllamaModelType.GEMMA2;

    public OllamaClient() {
        String host = "http://localhost:11434/";
        ollamaAPI = new OllamaAPI(host);
        ollamaAPI.setRequestTimeoutSeconds(60);
    }

    public String sendQuery(String query) throws InterruptedException, OllamaBaseException {
        OllamaAsyncResultStreamer streamer = ollamaAPI.generateAsync(MODEL, query, false);

        int pollIntervalMilliseconds = 1000;

        StringBuilder completeResponse = new StringBuilder();

        while (true) {
            String tokens = streamer.getStream().poll();
            if (tokens != null) {
                completeResponse.append(tokens);
            }

            if (!streamer.isAlive()) {
                break;
            }

            Thread.sleep(pollIntervalMilliseconds);
        }

        return completeResponse.toString();
    }
}
