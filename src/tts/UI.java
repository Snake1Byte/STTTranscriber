package tts;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.sound.sampled.LineUnavailableException;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicReference;

public class UI extends JFrame {
    private JComboBox<String> inputSelection;
    private Vector<String> inputs;

    private JComboBox<Languages> languagesSelection;
    private Vector<Languages> languages;

    private PanelButton controlButton;
    private JLabel controlButtonIcon;
    private ImageIcon recIcon;
    private ImageIcon stopIcon;
    private JLabel controlButtonText;

    private JList<LogElement> logList;
    private Vector<LogElement> log;
    private JScrollPane logScrollPane;

    private JTable resultTable;
    private JScrollPane tableScrollPane;
    private Vector<Vector<String>> resultVector;
    private Vector<String> columnNames;

    private File recordingToDelete;

    Recorder recorder;
    Transcriber transcriber;
    Translator translator;
    Persistence persistence;

    public UI() {
        super("Speech to Text translator");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        initializeLogArea();

        languages = new Vector<>();
        languages.add(Languages.GERMAN);
        languages.add(Languages.ITALIAN);
        languagesSelection = new JComboBox<>(languages);

        recIcon = new ImageIcon(new ImageIcon(getClass().getResource("/resources/rec.png")).getImage()
                .getScaledInstance(32, 32, Image.SCALE_DEFAULT));
        stopIcon = new ImageIcon(new ImageIcon(getClass().getResource("/resources/stop.png")).getImage()
                .getScaledInstance(32, 32, Image.SCALE_DEFAULT));

        controlButtonIcon = new JLabel(recIcon);
        controlButtonText = new JLabel("Record");
        controlButton = new PanelButton();

        inputs = new Vector<>();
        inputSelection = new JComboBox<>(inputs);

        transcriber = new Transcriber();
        transcriber.addTranscribeEventListener(transcribeEventListener);

        translator = new Translator();

        columnNames = new Vector<>();

        loadSettings();

        resultVector = new Vector<>();
        resultTable = new JTable(resultVector, columnNames);

        inputSelection.addItemListener(e -> {
            inputSelectionChanged((String) e.getItem());
        });
        languagesSelection.addItemListener(e -> {
            languageChanged((Languages) e.getItem());
        });
        controlButton.addActionListener(e -> {
            new Thread(this::recordOrStop, "Recording Thread").start();
        });

        buildUI();

        setVisible(true);
        setSize(500, 500);
    }

    private void initializeLogArea() {
        log = new Vector<>();
        logList = new JList<>(log);
        logScrollPane = new JScrollPane(logList);
        logList.setCellRenderer((list, value, index, isSelected, cellHasFocus) -> {
            JLabel label = new JLabel(value.message());
            label.setForeground(value.color());
            return label;
        });
        Logger.addLogListener((message, type) -> {
            Color color = null;
            switch (type) {
                case INFO -> color = Color.BLACK;
                case WARNING -> color = Color.ORANGE;
                case ERROR -> color = Color.RED;
            }
            log(message, color);
        });
    }

    private void loadSettings() {
        try {
            persistence = Persistence.instance();

            recorder = new Recorder(persistence.ROOT);
            inputs.addAll(recorder.getAudioInputDevices());

            String setLanguage = persistence.settings.get("language");
            if (setLanguage != null) {
                if (setLanguage.equals(Languages.GERMAN.getCode())) {
                    languagesSelection.setSelectedItem(Languages.GERMAN);
                    transcriber.setLanguage(Languages.GERMAN);
                    translator.setLanguage(Languages.GERMAN);
                    columnNames.add(Languages.GERMAN.getName());
                    columnNames.add(Languages.ENGLISH.getName());
                } else if (setLanguage.equals(Languages.ITALIAN.getCode())) {
                    languagesSelection.setSelectedItem(Languages.ITALIAN);
                    transcriber.setLanguage(Languages.ITALIAN);
                    translator.setLanguage(Languages.ITALIAN);
                    columnNames.add(Languages.ITALIAN.getName());
                    columnNames.add(Languages.ENGLISH.getName());
                }
            } else {
                Languages language = (Languages) languagesSelection.getSelectedItem();
                transcriber.setLanguage(language);
                translator.setLanguage(language);
                columnNames.add(Languages.ENGLISH.toString());
                columnNames.add(language.toString());
            }

            String audioDevice = persistence.settings.get("input");
            if (audioDevice != null) {
                inputSelection.setSelectedItem(audioDevice);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, String
                    .format("An error occurred while trying to create the necessary folders. %n%s", e));
            System.exit(1);
        }
    }

    private void buildUI() {
        BoxLayout layout = new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS);
        getContentPane().setLayout(layout);

        JLabel inputLabel = new JLabel("Select input device...");
        add(inputLabel);
        add(inputSelection);

        add(Box.createVerticalStrut(20));

        JPanel languagePanel = new JPanel();
        BoxLayout languagePanelLayout = new BoxLayout(languagePanel, BoxLayout.PAGE_AXIS);
        languagePanel.setLayout(languagePanelLayout);
        JLabel languageLabel = new JLabel("Select language...");
        languagePanel.add(languageLabel);
        languagePanel.add(languagesSelection);

        JButton toggleTranslationDirectionButton = new JButton(new ImageIcon(new ImageIcon(getClass()
                .getResource("/resources/arrows.png")).getImage().getScaledInstance(32, 32, Image.SCALE_DEFAULT)));
        toggleTranslationDirectionButton.setPreferredSize(new Dimension(32, 32));
        JLabel englishLabel = new JLabel("English");

        JPanel languageSwitchPanel = new JPanel();
        BoxLayout languageSwitchLayout = new BoxLayout(languageSwitchPanel, BoxLayout.LINE_AXIS);
        languageSwitchPanel.setLayout(languageSwitchLayout);
        languageSwitchPanel.add(new JLabel("From... "));
        languageSwitchPanel.add(languagePanel);
        languageSwitchPanel.add(toggleTranslationDirectionButton);
        languageSwitchPanel.add(new JLabel("to... "));
        languageSwitchPanel.add(englishLabel);
        add(languageSwitchPanel);

        toggleTranslationDirectionButton.addActionListener(e -> {
            Component[] components = languageSwitchPanel.getComponents();
            if (components[1] instanceof JPanel) {
                languageSwitchPanel.removeAll();
                languageSwitchPanel.add(new JLabel("From... "));
                languageSwitchPanel.add(englishLabel);
                languageSwitchPanel.add(toggleTranslationDirectionButton);
                languageSwitchPanel.add(new JLabel("to... "));
                languageSwitchPanel.add(languagePanel);

                String language = columnNames.get(1);
                columnNames.set(1, columnNames.get(0));
                columnNames.set(0, language);
                resultTable.updateUI();

                transcriber.setLanguage(Languages.ENGLISH);
            } else {
                languageSwitchPanel.removeAll();
                languageSwitchPanel.add(new JLabel("From... "));
                languageSwitchPanel.add(languagePanel);
                languageSwitchPanel.add(toggleTranslationDirectionButton);
                languageSwitchPanel.add(new JLabel("to... "));
                languageSwitchPanel.add(englishLabel);

                String language = columnNames.get(1);
                columnNames.set(1, columnNames.get(0));
                columnNames.set(0, language);
                resultTable.updateUI();

                transcriber.setLanguage((Languages) languagesSelection.getSelectedItem());
            }
            revalidate();
            repaint();
        });

        add(Box.createVerticalStrut(20));

        BoxLayout controlButtonLayout = new BoxLayout(controlButton, BoxLayout.PAGE_AXIS);
        controlButton.setLayout(controlButtonLayout);
        controlButton.add(controlButtonIcon);
        controlButton.add(controlButtonText);
        add(controlButton);

        tableScrollPane = new JScrollPane(resultTable);

        add(tableScrollPane);

        add(logScrollPane);
    }

    private void inputSelectionChanged(String item) {
        // TODO check how many times this is called
        recorder.setAudioInputDevice(item);
        persistence.settings.put("input", item);
    }

    private void languageChanged(Languages language) {
        // TODO check how many times this is called
        transcriber.setLanguage(language);
        translator.setLanguage(language);
        columnNames.remove(1);
        columnNames.add(language.toString());

        persistence.settings.put("language", language.getCode());
    }

    private void recordOrStop() {
        if (controlButtonText.getText().equals("Record")) {
            String audioDevice = persistence.settings.get("input");
            try {
                AtomicReference<Timer> timer = new AtomicReference<>();
                timer.set(new Timer(0, w -> {
                    controlButtonIcon.setIcon(stopIcon);
                    controlButtonText.setText("Recording...");
                    repaint();
                    timer.get().stop();
                }));
                timer.get().start();
                recorder.record(audioDevice);
            } catch (LineUnavailableException | IOException w) {
                Logger.log(w.toString(), Logger.Type.ERROR);
            }
        } else {
            try {
                File recording = recorder.stop();
                recordingToDelete = recording;
                AtomicReference<Timer> timer = new AtomicReference<>();
                timer.set(new Timer(0, w -> {
                    controlButton.setEnabled(false);
                    controlButtonIcon.setEnabled(false);
                    repaint();
                    timer.get().stop();
                }));
                timer.get().start();

                String transcription = transcriber.transcribeAudio(recording);
                StringSelection selection = new StringSelection(transcription);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);

                timer.set(new Timer(0, w -> {
                    controlButtonText.setText("Querying dict.cc...");
                    timer.get().stop();
                }));
                timer.get().start();

                Map<String, String> translations = translator.translate(transcription);
                SwingUtilities.invokeLater(() -> {
                    resultVector.clear();
                    if (translations == null) {
                        Vector<String> newColumn = new Vector<>();
                        newColumn.add(String.format("No translations found for \"%s\".", transcription));
                        newColumn.add("");
                        resultVector.add(newColumn);
                    } else {
                        for (Map.Entry<String, String> entry : translations.entrySet()) {
                            Vector<String> newColumn = new Vector<>();
                            newColumn.add(entry.getKey());
                            newColumn.add(entry.getValue());
                            resultVector.add(newColumn);
                        }
                    }
                    resultTable.updateUI();
                });

                timer.set(new Timer(0, w -> {
                    controlButtonIcon.setIcon(recIcon);
                    controlButtonText.setText("Record");
                    controlButton.setEnabled(true);
                    controlButtonIcon.setEnabled(true);
                    revalidate();
                    repaint();
                    timer.get().stop();
                }));
                timer.get().start();

            } catch (IOException | ProcessingException exception) {
                Logger.log(String.format("Error trying to transcribe audio. %s", exception), Logger.Type.ERROR);
                AtomicReference<Timer> timer = new AtomicReference<>();
                timer.set(new Timer(0, w -> {
                    controlButtonIcon.setIcon(recIcon);
                    controlButtonText.setText("Record");
                    controlButton.setEnabled(true);
                    controlButtonIcon.setEnabled(true);
                    revalidate();
                    repaint();
                    timer.get().stop();
                }));
                timer.get().start();
            }
        }
    }

    private void log(String line, Color color) {
        SwingUtilities.invokeLater(() -> {
            log.add(new LogElement(line, color));
            logList.updateUI();
            JScrollBar logScrollBar = logScrollPane.getVerticalScrollBar();
            logScrollBar.setValue(logScrollBar.getMaximum());
        });
    }

    private TranscribeEventListener transcribeEventListener = new TranscribeEventListener() {
        @Override
        public void uploading() {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    controlButtonText.setText("Uploading recording...");
                });
            } catch (InterruptedException | InvocationTargetException ignored) {
            }
        }

        @Override
        public void submittingAudio() {

            try {
                SwingUtilities.invokeAndWait(() -> {
                    controlButtonText.setText("Submitting recording...");
                });
            } catch (InterruptedException | InvocationTargetException ignored) {
            }
        }

        @Override
        public void transcriptionQueued() {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    controlButtonText.setText("Transcription queued.");
                });
            } catch (InterruptedException | InvocationTargetException ignored) {
            }
        }

        @Override
        public void transcriptionProcessing() {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    controlButtonText.setText("Transcription processing...");
                });
            } catch (InterruptedException | InvocationTargetException ignored) {
            }
        }

        @Override
        public void transcriptionDone() {
            try {
                SwingUtilities.invokeAndWait(() -> {
                    controlButtonText.setText("Transcription done.");
                });
                recordingToDelete.delete();
            } catch (InterruptedException | InvocationTargetException ignored) {
            }
        }
    };

    private static class PanelButton extends JPanel {
        private LinkedList<ActionListener> actionListeners;

        private Color initialColor;

        public PanelButton() {
            actionListeners = new LinkedList<>();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (PanelButton.this.isEnabled()) {
                        for (ActionListener l : actionListeners) {
                            l.actionPerformed(new ActionEvent(PanelButton.this, ActionEvent.ACTION_PERFORMED, ""));
                        }

                        initialColor = PanelButton.this.getBackground();
                        Color newColor = new Color(Math.max(0, initialColor.getRed() - 30), Math
                                .max(0, initialColor.getGreen() - 30), Math.max(0, initialColor.getBlue() - 30));
                        PanelButton.this.setBackground(newColor);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (PanelButton.this.isEnabled()) {
                        PanelButton.this.setBackground(initialColor);
                    }
                }
            });
        }

        public void addActionListener(ActionListener l) {
            actionListeners.add(l);
        }

        public void removeActionListener(ActionListener l) {
            actionListeners.remove(l);
        }
    }

    public static void main(String[] args) throws UnsupportedLookAndFeelException {
        UIManager.setLookAndFeel(new FlatDarculaLaf());
        new UI();
        Logger.addLogListener(new LogListener() {
            @Override
            public void newLog(String message, Logger.Type type) {
                System.out.println(message);
            }
        });
    }
}
