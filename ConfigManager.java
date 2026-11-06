package com.mycompany.chest_counter_aplication;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class ConfigManager {
    private final String iniFilePath = "contador_data.ini";
    private final String logFilePath = "historial_cofres.txt";

    public Properties loadProperties() {
        Properties prop = new Properties();
        File file = new File(iniFilePath);
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                prop.load(in);
            } catch (IOException ignored) {}
        }
        return prop;
    }

    public void saveProperties(Properties prop) {
        try (FileOutputStream out = new FileOutputStream(iniFilePath)) {
            prop.store(out, "Configuracion Contador Fortnite");
        } catch (IOException ignored) {}
    }

    public void logMatch(int chests) {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFilePath, true), StandardCharsets.UTF_8))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.println("[" + timestamp + "] Partida finalizada: " + chests + " cofres");
        } catch (IOException ignored) {}
    }
}