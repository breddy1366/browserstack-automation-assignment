package utils;

import okhttp3.*;

import java.io.IOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Translator {
    private static final OkHttpClient client = new OkHttpClient();

    public static String translate(String text) throws Exception {
        String url = "https://translation.googleapis.com/language/translate/v2?key=AIzaSyDDKXtiUXP_IM0pLSF31hmXOUUYjuzpkL8";

        MediaType mediaType = MediaType.parse("application/json");
        String bodyJson = "{\"q\":\"" + text + "\",\"target\":\"en\"}";
        RequestBody body = RequestBody.create(bodyJson, mediaType);

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
            JsonObject json = JsonParser.parseString(response.body().string()).getAsJsonObject();
            return json.getAsJsonObject("data")
                    .getAsJsonArray("translations")
                    .get(0)
                    .getAsJsonObject()
                    .get("translatedText").getAsString();
        }
    }
}



//AIzaSyDDKXtiUXP_IM0pLSF31hmXOUUYjuzpkL8