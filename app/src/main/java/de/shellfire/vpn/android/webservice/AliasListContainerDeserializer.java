package de.shellfire.vpn.android.webservice;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

import de.shellfire.vpn.android.model.Alias;
import de.shellfire.vpn.android.webservice.model.AliasListContainer;

public class AliasListContainerDeserializer implements JsonDeserializer<AliasListContainer> {

    @Override
    public AliasListContainer deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        if (jsonObject.has("aliaslist")) {
            JsonElement dataElement = jsonObject.get("aliaslist");
            Alias[] aliasArray = context.deserialize(dataElement, Alias[].class);


            AliasListContainer aliasListContainer = new AliasListContainer(aliasArray);
            return aliasListContainer;
        }

        return null;
    }
}