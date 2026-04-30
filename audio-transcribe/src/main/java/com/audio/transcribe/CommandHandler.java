package com.audio.transcribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class CommandHandler {

    private static final String WAKE_WORD = "sk";

    private final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    private final Map<String, RunnableCommand> commands = new LinkedHashMap<>();

    public CommandHandler() {
        commands.put("open youtube", () -> openUrlInChromeOrDefault("https://www.youtube.com"));
        commands.put("open google", () -> openUrlInChromeOrDefault("https://www.google.com"));
        commands.put("open notepad", () -> runApp("notepad"));
        commands.put("open calculator", () -> runApp("calc"));
        commands.put("open code", () -> runApp("code")); // VS Code
        commands.put("open chrome", () -> runApp("chrome"));
        commands.put("open spotify", () -> runApp("spotify"));
        commands.put("open whatsapp", () -> runApp("whatsapp"));
        commands.put("open file explorer", () -> runApp("explorer"));

    }




    public void handleTranscription(String text) {
        if (text == null || text.isBlank()) return;

        String lower = text.toLowerCase(Locale.ROOT).trim();

        if (!lower.startsWith(WAKE_WORD + " ")) {
            log.info("Wake word not detected. Ignoring command: {}", text);
            return;
        }

        String commandText = lower.substring((WAKE_WORD + " ").length());


        if (commandText.startsWith("search ")) {
            String query = commandText.substring("search ".length()).trim();
            if(query.startsWith("youtube")){
                String videoQuery=query.substring("youtube".length()).trim();
                String url="https://www.youtube.com/results?search_query="
                        + URLEncoder.encode(videoQuery,StandardCharsets.UTF_8);
                log.info("Performing YouTube search for: {}", videoQuery);
                openUrlInChromeOrDefault(url);
                return;
            }else {
                String url = "https://www.google.com/search?q=" +
                        URLEncoder.encode(query, StandardCharsets.UTF_8);

                log.info("Performing Google search for: {}", query);
                openUrlInChromeOrDefault(url);
                return;
            }
        }

        commands.forEach((key, cmd) -> {
            if (commandText.contains(key)) {
                log.info("Command '{}' detected after wake word. Executing...", key);
                try {
                    cmd.run();
                } catch (Exception e) {
                    log.error("Failed to execute command '{}': {}", key, e.getMessage());
                }
            }
        });

    }

    private void runApp(String appName) {
        try {
            new ProcessBuilder("cmd", "/c", "start", appName).start();
            log.info("Opening app: {}", appName);
        } catch (Exception e) {
            log.error("Failed to open app {}: {}", appName, e.getMessage());
        }
    }



    private void openUrlInChromeOrDefault(String url) {
        if (!tryOpenChrome(url)) {
            log.info("Falling back to default browser for URL: {}", url);
            try {
                openInDefaultBrowser(url);
            } catch (Exception e) {
                log.error("Failed to open URL in default browser: {}", e.getMessage(), e);
            }
        }
    }

    private boolean tryOpenChrome(String url) {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        log.debug("Detected OS: {}", os);

        try {
            if (os.contains("win")) {

                new ProcessBuilder("cmd", "/c", "start", "chrome", url).start();
                return true;
            } else if (os.contains("mac")) {

                new ProcessBuilder("open", "-a", "Google Chrome", url).start();
                return true;
            } else {
                String[] tryCmds = {
                        "google-chrome",
                        "google-chrome-stable",
                        "chromium-browser",
                        "chromium",
                        "chrome"
                };
                for (String cmd : tryCmds) {
                    try {
                        new ProcessBuilder(cmd, url).start();
                        return true;
                    } catch (IOException ignore) {
                    }
                }

                return false;
            }
        } catch (IOException e) {
            log.warn("Trying to start Chrome failed: {}", e.getMessage());
            return false;
        }
    }

    private void openInDefaultBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if (os.contains("linux")) {
                new ProcessBuilder("xdg-open", url).start();
            } else {
                throw new IllegalStateException("Cannot open browser on this platform");
            }
        }
    }

    @FunctionalInterface
    private interface RunnableCommand {
        void run();
    }
}

