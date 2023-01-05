package tts;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class Persistence {
    public SavingMap settings;
    public final File ROOT = FileSystemView.getFileSystemView().getDefaultDirectory().toPath().resolve("TTSTranslator")
            .toFile();

    private ObjectMapper mapper;
    private File persistenceFile = ROOT.toPath().resolve("persistence.json").toFile();
    private static Persistence unique = null;

    private boolean save;

    public static Persistence instance() throws IOException {
        if (unique == null) {
            unique = new Persistence();
        }
        return unique;
    }

    private Persistence() throws IOException {
        mapper = new ObjectMapper();

        if (!ROOT.exists()) {
            Files.createDirectory(ROOT.toPath());
        }
        if (!persistenceFile.exists()) {
            Files.createFile(persistenceFile.toPath());
            initializeSettingsWithDefaultValues();
            save = true;
        } else {
            try {
                settings = new SavingMap(mapper.readValue(persistenceFile, Map.class)); // oh no! anyway
                save = true;
            } catch (IOException e) {
                Logger.log(String
                        .format("Could not load settings. Using default values. Not saving any settings for this session. %s%n", e), Logger.Type.ERROR);
                initializeSettingsWithDefaultValues();
                save = false;
            }
        }
    }

    private void initializeSettingsWithDefaultValues() {
        settings = new SavingMap();
        settings.put("input", null);
        settings.put("language", null);
    }

    public class SavingMap extends HashMap<String, String> {
        public SavingMap(Map<String, String> map) {
            super(map);
        }

        public SavingMap() {
            super();
        }

        @Override
        public String put(String key, String value) {
            String old = super.put(key, value);
            if (save) {
                try {
                    mapper.writeValue(persistenceFile, this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return old;
        }
    }
}
