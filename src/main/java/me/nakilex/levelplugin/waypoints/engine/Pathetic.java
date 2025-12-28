package me.nakilex.levelplugin.waypoints.engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Pathetic {

  private static final String PROPERTIES_FILE = "pathetic.properties";
  private static final Logger LOGGER = Logger.getLogger(Pathetic.class.getName());

  private static String engineVersion;

  private Pathetic() {
    throw new AssertionError("Pathetic is a utility class and should not be instantiated");
  }

  private static void loadEngineVersion() {
    try (InputStream inputStream =
        Pathetic.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
      Properties properties = new Properties();
      properties.load(inputStream);

      engineVersion = properties.getProperty("engine.version");
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, "Error loading engine version", e);
      throw new IllegalStateException("Error loading engine version", e);
    }
  }

  public static String getOrLoadEngineVersion() {
    if (engineVersion == null) loadEngineVersion();
    return engineVersion;
  }
}
