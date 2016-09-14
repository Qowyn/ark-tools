package qowyn.ark.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class DataManager {

  private static final String DATA_FILE_NAME = "/ark_data";

  private static final String DATA_FILE_EXT = ".json";

  private static final Map<String, ArkCreature> CREATURE_DATA = new HashMap<>();

  private static final Map<String, ArkItem> ITEM_DATA = new HashMap<>();

  private static final Map<String, ArkItem> ITEM_DATA_BY_BGC = new HashMap<>();

  private static final Map<String, ArkCreature> STRUCTURES_DATA = new HashMap<>();

  public static void loadData(String language) {
    try {
      String fileName;
      if (language != null) {
        fileName = DATA_FILE_NAME + "_" + language + DATA_FILE_EXT;
      } else {
        fileName = DATA_FILE_NAME + DATA_FILE_EXT;
      }

      JsonObject data = (JsonObject) CommonFunctions.readJsonRelative(fileName);
      JsonArray creatures = data.getJsonArray("creatures");

      for (JsonObject entry : creatures.getValuesAs(JsonObject.class)) {
        String packagePath = entry.getString("package");
        String blueprint = entry.getString("blueprint");
        String clazz = entry.getString("class");
        String name = entry.getString("name");
        String category = entry.getString("category", null);

        CREATURE_DATA.put(clazz, new ArkCreature(name, clazz, blueprint, packagePath, category));
      }

      JsonArray items = data.getJsonArray("items");

      for (JsonObject entry : items.getValuesAs(JsonObject.class)) {
        String packagePath = entry.getString("package");
        String blueprint = entry.getString("blueprint");
        String clazz = entry.getString("class");
        String name = entry.getString("name");
        String category = entry.getString("category", null);

        String blueprintGeneratedClass = "BlueprintGeneratedClass " + packagePath + "." + clazz;

        ArkItem item = new ArkItem(name, blueprint, blueprintGeneratedClass, category);
        ITEM_DATA.put(clazz, item);
        ITEM_DATA_BY_BGC.put(blueprintGeneratedClass, item);
      }

      JsonArray structures = data.getJsonArray("structures");

      for (JsonObject entry : structures.getValuesAs(JsonObject.class)) {
        String packagePath = entry.getString("package");
        String blueprint = entry.getString("blueprint");
        String clazz = entry.getString("class");
        String name = entry.getString("name");
        String category = entry.getString("category", null);

        STRUCTURES_DATA.put(clazz, new ArkCreature(name, clazz, blueprint, packagePath, category));
      }
    } catch (IOException e) {
      System.err.println("Warning: Cannot load data.");
      e.printStackTrace();
    }
  }

  public static boolean hasCreature(String clazz) {
    return CREATURE_DATA.containsKey(clazz);
  }

  public static ArkCreature getCreature(String clazz) {
    return CREATURE_DATA.get(clazz);
  }

  public static boolean hasStructure(String clazz) {
    return STRUCTURES_DATA.containsKey(clazz);
  }

  public static ArkCreature getStructure(String clazz) {
    return STRUCTURES_DATA.get(clazz);
  }

  public static boolean hasItem(String clazz) {
    return ITEM_DATA.containsKey(clazz);
  }

  public static ArkItem getItem(String clazz) {
    return ITEM_DATA.get(clazz);
  }

  public static boolean hasItemByBGC(String blueprintGeneratedClass) {
    return ITEM_DATA_BY_BGC.containsKey(blueprintGeneratedClass);
  }

  public static ArkItem getItemByBGC(String blueprintGeneratedClass) {
    return ITEM_DATA_BY_BGC.get(blueprintGeneratedClass);
  }

}
