/**
 * This file is part of kt2l-core.
 *
 * kt2l-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * kt2l-core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with kt2l-core.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.mhus.kt2l.ai;

import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.commons.tools.MString;
import de.mhus.kt2l.config.Configuration;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class AiService {

    private static final String OLLAMA = "ollama";
    private static final String OPENAI = "openai";
    public static final String MODEL_MISTRAL = OLLAMA + ":mistral:";
    public static final String MODEL_CODELLAMA = OLLAMA + ":codellama:";
    public static final String MODEL_LLAMA2 = OLLAMA + ":llama2:";
    public static final String MODEL_OPENCHAT = OLLAMA + ":openchat:";
    public static final String MODEL_STARCODER = OLLAMA + ":starcoder:";
    public static final String MODEL_STARCODER2 = OLLAMA + ":starcoder2:";
    public static final String MODEL_YI = OLLAMA + ":yi:";

    private Map<String, ChatLanguageModel> models = new HashMap<>();

    @Autowired
    AiConfiguration config;

    public synchronized ChatLanguageModel getModel(String modelName) {
        String[] parts = modelName.split(":");
        if (parts[0].equals(OLLAMA)) {
            return models.computeIfAbsent(parts[1], name -> {
                ChatLanguageModel model = OllamaChatModel.builder()
                        .baseUrl(config.getOllamaUrl())
                        .modelName(name)
                        .build();
                return model;
            });
        } else if (parts[0].equals(OPENAI)) {
            return models.computeIfAbsent(parts[1], name -> {
                var openAiKey = config.getOpenAiKey();
                if (MString.isEmpty(openAiKey))
                    throw new NotFoundRuntimeException("OpenAi key not configured in ai configuration");
                ChatLanguageModel model = OpenAiChatModel.builder()
                        .modelName(name)
                        .apiKey(openAiKey)
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

    public Response<AiMessage> generate(String model, Prompt prompt) {
        Response<AiMessage> response = getModel(model).generate(prompt.toUserMessage());
        return response;
    }

    public Optional<String> getTemplateForPrompt(String name) {
        return config.getTemplate(name);
    }

    public Optional<String> getModelForPrompt(String name) {
        return config.getModel(name);
    }

}
