package tts;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Translator {
    private HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    private Languages languages;

    public Map<String, String> translate(String text) throws IOException {
        try {
            URI uri;
            if (languages.equals(Languages.GERMAN)) {
                uri = new URI(String
                        .format("https://www.dict.cc/?s=%s", URLEncoder.encode(text, Charset.defaultCharset())));
            } else {
                uri = new URI(String
                        .format("https://enit.dict.cc/?s=%s", URLEncoder.encode(text, Charset.defaultCharset())));
            }
            HttpRequest req = HttpRequest.newBuilder().uri(uri).GET().build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            printResponse(res);

            String body = res.body();
            Pattern pattern = Pattern.compile("c1Arr = new Array\\((.*)\\);\\s*var c2Arr = new Array\\((.*)\\)");
            Matcher m = pattern.matcher(body);
            if (m.find()) {
                String[] english = m.group(1).split(",");
                String[] german = m.group(2).split(",");
                List<String> englishWords = Arrays.stream(english).map(e -> e.substring(1, e.length() - 1))
                        .collect(Collectors.toList());
                List<String> germanWords = Arrays.stream(german).map(e -> e.substring(1, e.length() - 1))
                        .collect(Collectors.toList());
                Map<String, String> dictionary = new LinkedHashMap<>();
                for (int i = 0; i < englishWords.size(); ++i) {
                    if (germanWords.size() > i) {
                        dictionary.put(englishWords.get(i), germanWords.get(i));
                    } else {
                        dictionary.put(englishWords.get(i), "");
                    }
                }
                return dictionary;
            }
        } catch (URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setLanguage(Languages language) {
        this.languages = language;
    }

    private <T> void printResponse(HttpResponse<T> response) {
        System.out.println("HEADERS");
        response.headers().map().forEach((key, value) -> System.out.printf("%s\t\t\t\t\t\t%s%n", key, value));
        System.out.println("\nBODY");
        System.out.println(response.body());
        System.out.println("\n\n");
    }
}
