package de.mhus.kt2l.ai;

import de.mhus.commons.errors.NotFoundException;
import de.mhus.commons.errors.NotFoundRuntimeException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Component
public class AiService {

    private static final String OLLAMA = "ollama";
    public static final String MODEL_MISTRAL = OLLAMA + ":mistral:";
    public static final String MODEL_CODELLAMA = OLLAMA + ":codellama:";
    public static final String MODEL_LLAMA2 = OLLAMA + ":llama2:";
    public static final String MODEL_OPENCHAT = OLLAMA + ":openchat:";
    public static final String MODEL_STARCODER = OLLAMA + ":starcoder:";
    public static final String MODEL_STARCODER2 = OLLAMA + ":starcoder2:";
    public static final String MODEL_YI = OLLAMA + ":yi:";

    private Map<String, ChatLanguageModel> models = new HashMap<>();

    public synchronized ChatLanguageModel getModel(String modelName) {
        String[] parts = modelName.split(":");
        if (parts[0].equals(OLLAMA)) {
            return models.computeIfAbsent(parts[1], name -> {
                ChatLanguageModel model = OllamaChatModel.builder()
                        .baseUrl(ollamaBaseUrl())
                        .modelName(name)
                        .build();
                return model;
            });
        }
        throw new NotFoundRuntimeException("Model not found: " + modelName);
    }

    public Response<AiMessage> generate(String model, String ... questions) {
        Response<AiMessage> response = getModel(model).generate(Arrays.stream(questions).map(q -> (ChatMessage) UserMessage.from(q)).toList());
        return response;
    }

    static String ollamaBaseUrl() {
//        return String.format("http://%s:%d", ollama.getHost(), ollama.getFirstMappedPort());
        return String.format("http://%s:%d", "127.0.0.1", 11434);
    }

}
