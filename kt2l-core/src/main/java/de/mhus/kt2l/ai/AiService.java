/*
 * kt2l-core - kt2l core implementation
 * Copyright © 2024 Mike Hummel (mh@mhus.de)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.mhus.kt2l.ai;

import de.mhus.commons.errors.NotFoundRuntimeException;
import de.mhus.commons.tools.MString;
import de.mhus.commons.tree.IProperties;
import de.mhus.commons.util.MUri;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static de.mhus.commons.tools.MString.isBlank;
import static java.time.Duration.ofSeconds;

@Component
public class AiService {

    public static final String OLLAMA = "ollama";
    public static final String OPENAI = "openai";
    public static final String AUTO = "auto";
    public static final String AUTO_CODING = AUTO + "/coding/";
    public static final String AUTO_TRANSLATE = AUTO + "/translate/";

    public static final String MODEL_CODELLAMA = OLLAMA + "/codellama/";
    public static final String MODEL_YI = OLLAMA + "/yi/";

    public static final String MODEL_GPT_3_5_TURBO = OPENAI + "/gpt-3.5-turbo/";

    private Map<String, ChatLanguageModel> models = new HashMap<>();

    @Autowired
    AiConfiguration config;

    public synchronized ChatLanguageModel getModel(String modelName) {
        String[] parts = modelName.split("/", 3);
        if (parts.length < 2) {
            throw new NotFoundRuntimeException("Model name not valid: " + modelName);
        }
        String optionsStr = parts.length == 3 ? parts[2] : "";
        if (parts[0].equalsIgnoreCase(AUTO)) {
            if (parts[1].equalsIgnoreCase("coding")) {
                if (!isBlank(config.getDefaultCodingModel()))
                    return getModel(config.getDefaultCodingModel());
                if (!isBlank(config.getOpenAiKey()))
                    return getModel(OPENAI + optionsStr);
                else
                    return getModel(MODEL_CODELLAMA + optionsStr);
            } else if (parts[1].equalsIgnoreCase("translate")) {
                if (!isBlank(config.getDefaultTranslateModel()))
                    return getModel(config.getDefaultTranslateModel());
                if (!isBlank(config.getOpenAiKey()))
                    return getModel(MODEL_GPT_3_5_TURBO + optionsStr);
                else
                    return getModel(MODEL_YI + optionsStr);
            }
        }

        var options = IProperties.explodeToOptions(optionsStr);

        if (parts[0].equalsIgnoreCase(OLLAMA)) {
            return models.computeIfAbsent(parts[1], name -> {
                ChatLanguageModel model = OllamaChatModel.builder()
                        .baseUrl(config.getOllamaUrl())
                        .timeout(ofSeconds(options.getInt("timeout", 60)))
                        .temperature(options.getDouble("temperature", 0.3))
                        .modelName(name)
                        .build();
                return model;
            });
        } else if (parts[0].equalsIgnoreCase(OPENAI)) {
            return models.computeIfAbsent(parts[1], name -> {
                var openAiKey = config.getOpenAiKey();
                if (MString.isEmpty(openAiKey))
                    throw new NotFoundRuntimeException("OpenAi key not configured in ai configuration");
                ChatLanguageModel model = OpenAiChatModel.builder()
                        .modelName(name)
                        .timeout(ofSeconds(options.getInt("timeout", 60)))
                        .temperature(options.getDouble("temperature", 0.3))
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
