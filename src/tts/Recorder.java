package tts;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Recorder {
    AudioFormat audioFormat = new AudioFormat(48000, 16, 1, true, false);
    private Mixer audioInputDevice;
    private TargetDataLine targetDataLine;
    private File currentlyRecordedAudioFile;

    private File rootFolder;

    public Recorder(File rootFolder) {
        this.rootFolder = rootFolder;
    }

    public void record(String audioDeviceName) throws LineUnavailableException, IOException {
        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
        if (audioDeviceName == null) {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
        } else {
            Mixer.Info info = Arrays.stream(AudioSystem.getMixerInfo()).filter(e -> !e.getName().startsWith("Port"))
                    .filter(e -> e.getDescription().equals("Direct Audio Device: DirectSound Capture"))
                    .filter(mixer -> mixer.getName().startsWith(audioDeviceName)).findFirst().orElseGet(() -> null);
            if (info != null) {
                audioInputDevice = AudioSystem.getMixer(info);
                targetDataLine = (TargetDataLine) audioInputDevice.getLine(dataLineInfo);
            } else {
                Logger.log(String
                        .format("Could not find audio input device %s. Defaulting to system default input device.%n", audioDeviceName), Logger.Type.ERROR);
                targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            }
        }

        currentlyRecordedAudioFile = rootFolder.toPath().resolve(String.format("%d%s", new Random().nextInt(1000000), ".wav"))
                .toFile();

        Logger.log("RECORDING...", Logger.Type.INFO);
        targetDataLine.open(audioFormat);
        targetDataLine.start();
        try {
            AudioSystem
                    .write(new AudioInputStream(targetDataLine), AudioFileFormat.Type.WAVE, currentlyRecordedAudioFile);
        } finally {
            Logger.log("STOPPING RECORDING.", Logger.Type.INFO);
            targetDataLine.stop();
            targetDataLine.close();
        }
    }

    public File stop() {
        if (targetDataLine.isOpen()) {
            Logger.log("STOPPING RECORDING.", Logger.Type.INFO);
            targetDataLine.stop();
            targetDataLine.close();
        }

        return currentlyRecordedAudioFile;
    }

    public List<String> getAudioInputDevices() {
        return Arrays.stream(AudioSystem.getMixerInfo()).filter(e -> !e.getName().startsWith("Port"))
                .filter(e -> e.getDescription().equals("Direct Audio Device: DirectSound Capture"))
                .map(Mixer.Info::getName).collect(Collectors.toList());
    }

    public void setAudioInputDevice(String audioInputDevice) {
        Mixer.Info info = Arrays.stream(AudioSystem.getMixerInfo()).filter(e -> e.getName().contains(audioInputDevice))
                .findFirst().get();
        this.audioInputDevice = AudioSystem.getMixer(info);
    }
}
