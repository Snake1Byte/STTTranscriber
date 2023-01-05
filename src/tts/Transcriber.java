package tts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;

public class Transcriber {
    private HttpClient client = HttpClient.newBuilder().build();
    private Constants constants;
    private ObjectMapper mapper;
    private Languages language;

    private LinkedList<TranscribeEventListener> transcribeEventListeners;

    public Transcriber() {
        try {
            transcribeEventListeners = new LinkedList<>();
            constants = Constants.instance();
            mapper = new ObjectMapper();
        } catch (IOException ignored) {
            // this will not be thrown anyway since it's singleton and another class catches it first
        }
    }

    public void setLanguage(Languages newLanguage) {
        language = newLanguage;
    }

    public String transcribeAudio(File audioFile) throws IOException, ProcessingException {
        URI audioFileURI = upload(audioFile);

        return transcribe(audioFileURI);
    }

    private URI upload(File audioFile) throws IOException {
        Logger.log("UPLOADING.", Logger.Type.INFO);
        for (TranscribeEventListener l : transcribeEventListeners) {
            l.uploading();
        }

        try {
            HttpRequest req = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofFile(audioFile.toPath()))
                    .uri(new URI("https://api.assemblyai.com/v2/upload"))
                    .header("authorization", constants.getApiToken()).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            printResponse(res);

            String uploadURL = mapper.readTree(res.body()).get("upload_url").asText();
            return new URI(uploadURL);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (InterruptedException ignored) {
        }
        return null;
    }

    private String transcribe(URI audioFile) throws IOException, ProcessingException {
        Logger.log("SUBMITTING AUDIO.", Logger.Type.INFO);
        for (TranscribeEventListener l : transcribeEventListeners) {
            l.submittingAudio();
        }
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(new URI("https://api.assemblyai.com/v2/transcript"))
                    .header("authorization", constants.getApiToken()).header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(String
                            .format("{\"audio_url\": \"%s\", \"language_code\": \"%s\"}", audioFile, language
                                    .getCode()))).build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
            printResponse(res);

            JsonNode response = mapper.readTree(res.body());
            String status = response.get("status").asText();
            if (status.equals("error")) {
                throw new ProcessingException("AssemblyAI returned error.");
            }
            String id = response.get("id").asText();


            Logger.log("AWAITING COMPLETION.", Logger.Type.INFO);
            String lastStatus = "";
            while (true) {
                req = HttpRequest.newBuilder()
                        .uri(new URI(String.format("https://api.assemblyai.com/v2/transcript/%s", id)))
                        .header("authorization", constants.getApiToken()).GET().build();
                res = client.send(req, HttpResponse.BodyHandlers.ofString());
                printResponse(res);

                response = mapper.readTree(res.body());
                status = response.get("status").asText();
                if (status.equals("error")) {
                    throw new ProcessingException("AssemblyAI returned error.");
                } else if (status.equals("completed")) {
                    for (TranscribeEventListener l : transcribeEventListeners) {
                        l.transcriptionDone();
                    }
                    String transcribedWord = response.get("text").asText();
                    Logger.log(String.format("Transcribed word: \"%s\"", transcribedWord), Logger.Type.INFO);
                    return transcribedWord;
                } else if (status.equals("queued") && !lastStatus.equals("queued") && !lastStatus
                        .equals("processing")) {
                    lastStatus = "queued";
                    for (TranscribeEventListener l : transcribeEventListeners) {
                        l.transcriptionQueued();
                    }
                } else if (status.equals("processing") && !lastStatus.equals("processing")) {
                    lastStatus = "processing";
                    for (TranscribeEventListener l : transcribeEventListeners) {
                        l.transcriptionProcessing();
                    }
                }

                Thread.sleep(1000);
            }
        } catch (URISyntaxException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private <T> void printResponse(HttpResponse<T> response) {
        System.out.println("HEADERS");
        response.headers().map().forEach((key, value) -> System.out.printf("%s\t\t\t\t\t\t%s%n", key, value));
        System.out.println("\nBODY");
        System.out.println(response.body());
        System.out.println("\n\n");
    }

    public void addTranscribeEventListener(TranscribeEventListener l) {
        transcribeEventListeners.add(l);
    }

    public void removeTranscribeEventListener(TranscribeEventListener l) {
        transcribeEventListeners.remove(l);
    }
}
