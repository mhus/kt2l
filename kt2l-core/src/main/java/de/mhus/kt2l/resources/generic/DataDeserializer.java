package de.mhus.kt2l.resources.generic;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class DataDeserializer implements JsonDeserializer<String>, JsonSerializer<String> {
    @Override
    public String deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (jsonElement.isJsonObject()) {
            return jsonElement.getAsJsonObject().toString();
        } else if (jsonElement.isJsonPrimitive()) {
            return jsonElement.getAsString();
        }
        return "";
    }

    @Override
    public JsonElement serialize(String s, Type type, JsonSerializationContext jsonSerializationContext) {
        return JsonParser.parseString(s)
                .getAsJsonObject();
    }
}
