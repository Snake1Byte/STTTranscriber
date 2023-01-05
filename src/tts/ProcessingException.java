package tts;

public class ProcessingException extends Exception{
    public ProcessingException(Exception e) {
        super(e);
    }

    public ProcessingException(String message) {
        super(message);
    }
}
