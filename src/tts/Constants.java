package tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

public class Constants {
    private String apiToken;
    private static Constants instance = null;
    private ObjectMapper mapper = new ObjectMapper();

    public static Constants instance() throws IOException {
        if (instance == null) {
            instance = new Constants();
        }
        return instance;
    }

    private Constants() throws IOException {
        InputStream stream = Constants.class.getResourceAsStream("/resources/constants.json");
        JsonNode node = mapper.readTree(stream);
        apiToken = node.get("api_token").asText();
    }

    public String getApiToken() {
        return apiToken;
    }
}
