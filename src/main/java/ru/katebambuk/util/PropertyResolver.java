package ru.katebambuk.util;


import java.io.FileInputStream;
import java.util.Properties;

public class PropertyResolver {

    private PropertyResolver() {
        // util class
    }

    private static int processingDuration = 1;
    private static int sendingDuration = 1;


    public static void loadProperties() {
        Properties property = new Properties();

        try (FileInputStream  fis = new FileInputStream("src/main/resources/config.properties")) {
            property.load(fis);

            processingDuration = Integer.parseInt(property.getProperty("duration.processing"));
            sendingDuration = Integer.parseInt(property.getProperty("duration.sending"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getProcessingDuration() {
        return processingDuration;
    }

    public static int getSendingDuration() {
        return sendingDuration;
    }
}
