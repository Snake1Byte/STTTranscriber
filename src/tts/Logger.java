package tts;

import java.util.LinkedList;

public class Logger {
    private static LinkedList<LogListener> listeners = new LinkedList<>();


    public static void log(String message, Logger.Type type) {
        sendEvent(message, type);
    }

    private static void sendEvent(String message, Type type) {
        for (LogListener l : listeners) {
            l.newLog(message, type);
        }
    }

    public static void addLogListener(LogListener l) {
        listeners.add(l);
    }

    public static void removeLogListener(LogListener l) {
        listeners.remove(l);
    }

    public enum Type {
        INFO, WARNING, ERROR
    }

}
