# intellij-autocomplete-plugin

## Download Ollama Model
To use an Ollama model in this project, follow these steps:

1. Download the desired Ollama model from the [Ollama4J repository](https://github.com/ollama4j/ollama4j).

2. Pull the model you intend to use by running the following command in your terminal or command prompt (for example, to download the `gemma2` model):

```bash
ollama pull gemma2
```


3. You can choose any model, but if you use a different one, make sure to update the model name in the `OllamaClient` class accordingly.