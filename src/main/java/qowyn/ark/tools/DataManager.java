package qowyn.ark.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class DataManager {

  private static final String DATA_FILE_NAME = "/ark_data";

  private static final String DATA_FILE_EXT = ".json";

  private static final Map<String, CreatureData> CREATURE_DATA = new HashMap<>();

  private static final Map<String, CreatureData> CREATURE_DATA_BY_PATH = new HashMap<>();

  private static final Map<String, ItemData> ITEM_DATA = new HashMap<>();

  private static final Map<String, ItemData> ITEM_DATA_BY_BGC = new HashMap<>();

  private static final Map<String, CreatureData> STRUCTURES_DATA = new HashMap<>();

  public static void loadData(String language) {
    try {
      String fileName;
      if (language != null) {
        fileName = DATA_FILE_NAME + "_" + language + DATA_FILE_EXT;
      } else {
        fileName = DATA_FILE_NAME + DATA_FILE_EXT;
      }

      JsonNode data;

      try {
        data = CommonFunctions.readJsonRelative(fileName);
      } catch (FileNotFoundException fnfe) {
        throw new RuntimeException("Unable to load data file ." + fileName);
      }

      JsonNode creatures = data.get("creatures");

      for (JsonNode entry : creatures) {
        String packagePath = entry.path("package").asText();
        String blueprint = entry.path("blueprint").asText();
        String clazz = entry.path("class").asText();
        String name = entry.path("name").asText();
        String category = entry.path("category").asText();

        CreatureData creature = new CreatureData(name, clazz, blueprint, packagePath, category);
        CREATURE_DATA.put(clazz, creature);
        CREATURE_DATA_BY_PATH.put(packagePath + "." + clazz, creature);
      }

      JsonNode items = data.get("items");

      for (JsonNode entry : items) {
        String packagePath = entry.path("package").asText();
        String blueprint = entry.path("blueprint").asText();
        String clazz = entry.path("class").asText();
        String name = entry.path("name").asText();
        String category = entry.path("category").asText();

        String blueprintGeneratedClass = "BlueprintGeneratedClass " + packagePath + "." + clazz;

        ItemData item = new ItemData(name, blueprint, blueprintGeneratedClass, category);
        ITEM_DATA.put(clazz, item);
        ITEM_DATA_BY_BGC.put(blueprintGeneratedClass, item);
      }

      JsonNode structures = data.get("structures");

      for (JsonNode entry : structures) {
        String packagePath = entry.path("package").asText();
        String blueprint = entry.path("blueprint").asText();
        String clazz = entry.path("class").asText();
        String name = entry.path("name").asText();
        String category = entry.path("category").asText();

        STRUCTURES_DATA.put(clazz, new CreatureData(name, clazz, blueprint, packagePath, category));
      }
    } catch (IOException e) {
      System.err.println("Warning: Cannot load data.");
      e.printStackTrace();
    }
  }

  public static boolean hasCreature(String clazz) {
    return CREATURE_DATA.containsKey(clazz);
  }

  public static CreatureData getCreature(String clazz) {
    return CREATURE_DATA.get(clazz);
  }

  public static boolean hasCreatureByPath(String clazz) {
    return CREATURE_DATA_BY_PATH.containsKey(clazz);
  }

  public static CreatureData getCreatureByPath(String clazz) {
    return CREATURE_DATA_BY_PATH.get(clazz);
  }

  public static boolean hasStructure(String clazz) {
    return STRUCTURES_DATA.containsKey(clazz);
  }

  public static CreatureData getStructure(String clazz) {
    return STRUCTURES_DATA.get(clazz);
  }

  public static boolean hasItem(String clazz) {
    return ITEM_DATA.containsKey(clazz);
  }

  public static ItemData getItem(String clazz) {
    return ITEM_DATA.get(clazz);
  }

  public static boolean hasItemByBGC(String blueprintGeneratedClass) {
    return ITEM_DATA_BY_BGC.containsKey(blueprintGeneratedClass);
  }

  public static ItemData getItemByBGC(String blueprintGeneratedClass) {
    return ITEM_DATA_BY_BGC.get(blueprintGeneratedClass);
  }

}
