# Intellij Autocomplete Plugin

<!-- Plugin description -->
The IntelliJ Autocomplete Plugin enhances your coding experience by providing AI-powered code suggestions within IntelliJ IDEA using a local Ollama model.
<!-- Plugin description end -->

## Download Ollama Model
To use an Ollama model in this project, follow these steps:

1. Download the desired Ollama model from the [Ollama4J repository](https://github.com/ollama4j/ollama4j).

2. Pull the model you intend to use by running the following command in your terminal or command prompt (for example, to download the `gemma2` model):

```bash
ollama pull gemma2
```

3. You can choose any model, but if you use a different one, make sure to update the model name in the `OllamaClient` class accordingly.

4. Run the Ollama model using:
```bash
ollama start gemma2
```

## Example usage
### Example 1: Java Code Completion

```java
public class Example {
    public static void main(String[] args) {
    // Start typing "System.o"
    System.o
    // Completion: "ut.println();" is suggested
    }
}
```

### Example 2: Kotlin Code Completion

```kotlin
fun main() {
    val name = "Kotlin"
    // Start typing "println("
    println(
    // Completion: "name)" is suggested
}
```

### Caching Example
The plugin caches suggestions to improve performance. Here’s how it works:

1. When you type `System.o`, the plugin fetches the completion `System.out.println();` from the AI model.
2. If you type `System.o` or any <b>substring</b> again, the plugin serves the cached suggestion instantly, avoiding another AI query.