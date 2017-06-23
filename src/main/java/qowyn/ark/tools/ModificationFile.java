package qowyn.ark.tools;

import static qowyn.ark.tools.JsonValidator.expect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import qowyn.ark.tools.data.Item;
import qowyn.ark.types.ArkName;

public class ModificationFile {

  public final Map<String, String> remapDinoClassName = new HashMap<>();

  public final Map<ArkName, ArkName> remapItemArchetypes = new HashMap<>();

  public final Set<ArkName> removeItems = new HashSet<>();

  public final List<Item> addItems = new ArrayList<>();

  public final Map<ArkName, List<Item>> replaceDefaultInventoriesMap = new HashMap<>();

  public final Map<ArkName, List<Item>> replaceInventoriesMap = new HashMap<>();

  public final Map<ArkName, List<Item>> addDefaultInventoriesMap = new HashMap<>();

  public final Map<ArkName, List<Item>> addInventoriesMap = new HashMap<>();

  public void readJson(JsonObject object) {
    
    // Cluster and LocalProfile start

    JsonValue dinoClassNamesValue = object.get("remapDinoClassNames");

    if (expect(dinoClassNamesValue, "remapDinoClassNames", JsonValue.ValueType.OBJECT)) {
      JsonObject dinoClassNames = (JsonObject) dinoClassNamesValue;

      dinoClassNames.forEach((name, value) -> {
        if (expect(value, name, JsonValue.ValueType.STRING)) {
          remapDinoClassName.put(name, ((JsonString) value).getString());
        }
      });
    }

    JsonValue itemArchetypesValue = object.get("remapItemArchetypes");

    if (expect(itemArchetypesValue, "remapItemArchetypes", JsonValue.ValueType.OBJECT)) {
      JsonObject itemArchetypes = (JsonObject) itemArchetypesValue;

      itemArchetypes.forEach((name, value) -> {
        if (expect(value, name, JsonValue.ValueType.STRING)) {
          remapItemArchetypes.put(ArkName.from(name), ArkName.from(((JsonString) value).getString()));
        }
      });
    }

    JsonValue removeItemsValue = object.get("removeItems");

    if (expect(removeItemsValue, "removeItems", JsonValue.ValueType.ARRAY)) {
      for (JsonString itemClass : ((JsonArray) removeItemsValue).getValuesAs(JsonString.class)) {
        removeItems.add(ArkName.from(itemClass.getString()));
      }
    }

    JsonValue addItemsValue = object.get("addItems");

    if (expect(addItemsValue, "addItems", JsonValue.ValueType.ARRAY)) {
      JsonArray itemArray = (JsonArray) addItemsValue;
      for (JsonObject item : itemArray.getValuesAs(JsonObject.class)) {
        addItems.add(new Item(item));
      }
    }
    
    // Cluster and LocalProfile end
    
    // Map start

    BiConsumer<String, Map<ArkName, List<Item>>> inventoryLoader = (fieldName, map) -> {
      JsonValue inventoriesValue = object.get(fieldName);

      if (expect(inventoriesValue, fieldName, JsonValue.ValueType.OBJECT)) {
        JsonObject inventories = (JsonObject) inventoriesValue;

        inventories.forEach((name, value) -> {
          if (expect(value, name, JsonValue.ValueType.ARRAY)) {
            JsonArray itemArray = (JsonArray) value;
            List<Item> items = new ArrayList<>();

            for (JsonObject item : itemArray.getValuesAs(JsonObject.class)) {
              items.add(new Item(item));
            }

            map.put(ArkName.from(name), items);
          }
        });
      }
    };

    //inventoryLoader.accept("replaceDefaultInventoryItems", replaceDefaultInventoriesMap);
    //inventoryLoader.accept("replaceInventoryItems", replaceInventoriesMap);
    inventoryLoader.accept("addToDefaultInventory", addDefaultInventoriesMap);
    inventoryLoader.accept("addToInventory", addInventoriesMap);
    
    // Map end
  }

}
