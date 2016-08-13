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

  public static final Map<String, ArkCreature> CREATURE_DATA = new HashMap<>();

  public static final Map<String, ArkItem> ITEM_DATA = new HashMap<>();

  public static void loadData() {
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
        String blueprint = entry.getString("path", null);
        String category = entry.getString("category");

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
        String blueprint = entry.getString("path");
        String category = entry.getString("category");

        Matcher matcher = ITEM_CLASS_PATTERN.matcher(blueprint);
        if (!matcher.matches()) {
          continue;
        }

        String clazz = matcher.group(1) + "_C";

        ITEM_DATA.put(clazz, new ArkItem(name, blueprint, category));
      }
    } catch (IOException e) {
      System.err.println("Warning: Cannot load item data.");
      e.printStackTrace();
    }
  }

}
