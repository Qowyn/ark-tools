package qowyn.ark.tools;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonArray;
import javax.json.JsonObject;

public class DataManager {

  private static final String CREATURE_FILE_NAME = "entity_ids_creatures.json";

  private static final String ITEM_FILE_NAME = "entity_ids_items.json";

  private static final Pattern ITEM_CLASS_PATTERN = Pattern.compile("[^.]+\\.(.+)'\"");

  private static final Pattern BLUEPRINT_PATTERN = Pattern.compile("\"Blueprint'([^']+)'\"");

  private static final Map<String, ArkCreature> CREATURE_DATA = new HashMap<>();

  private static final Map<String, ArkItem> ITEM_DATA = new HashMap<>();

  private static final Map<String, ArkItem> ITEM_DATA_BY_BGC = new HashMap<>();

  static {
    loadCreatureData();
    loadItemData();
  }

  private static void loadCreatureData() {
    CREATURE_DATA.clear();
    try {
      JsonObject data = (JsonObject) CommonFunctions.readJson(CREATURE_FILE_NAME);
      JsonArray creatures = data.getJsonArray("creatures");

      for (JsonObject entry : creatures.getValuesAs(JsonObject.class)) {
        String id = entry.getString("id").trim();

        if (id.isEmpty()) {
          continue;
        }

        String name = entry.getString("name");
        String blueprintString = entry.getString("path", null);
        String category = entry.getString("category");

        if (blueprintString == null) {
          continue;
        }

        Matcher matcher = BLUEPRINT_PATTERN.matcher(blueprintString);

        if (!matcher.matches() || matcher.groupCount() != 1) {
          continue;
        }

        String blueprint = matcher.group(1);

        CREATURE_DATA.put(id, new ArkCreature(name, id, blueprint, category));
      }
    } catch (IOException e) {
      System.err.println("Warning: Cannot load creature data.");
      e.printStackTrace();
    }
  }

  private static void loadItemData() {
    ITEM_DATA.clear();
    try {
      JsonObject data = (JsonObject) CommonFunctions.readJson(ITEM_FILE_NAME);
      JsonArray items = data.getJsonArray("items");

      for (JsonObject entry : items.getValuesAs(JsonObject.class)) {
        String name = entry.getString("name");
        String blueprintString = entry.getString("path");
        String category = entry.getString("category");

        Matcher matcher = ITEM_CLASS_PATTERN.matcher(blueprintString);
        if (!matcher.matches()) {
          continue;
        }

        String clazz = matcher.group(1) + "_C";

        Matcher blueprintMatcher = BLUEPRINT_PATTERN.matcher(blueprintString);

        if (!blueprintMatcher.matches()) {
          continue;
        }

        String blueprint = blueprintMatcher.group(1);
        String blueprintGeneratedClass = "BlueprintGeneratedClass " + blueprint + "_C";

        ArkItem item = new ArkItem(name, blueprint, blueprintGeneratedClass, category);
        ITEM_DATA.put(clazz, item);
        ITEM_DATA_BY_BGC.put(blueprintGeneratedClass, item);
      }
    } catch (IOException e) {
      System.err.println("Warning: Cannot load item data.");
      e.printStackTrace();
    }
  }

  public static boolean hasCreature(String clazz) {
    return CREATURE_DATA.containsKey(clazz);
  }

  public static ArkCreature getCreature(String clazz) {
    return CREATURE_DATA.get(clazz);
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
