package tts;

public interface TranscribeEventListener {
    void uploading();

    void submittingAudio();

    void transcriptionQueued();

    void transcriptionProcessing();

    void transcriptionDone();
}
