package qowyn.ark.tools;

import static qowyn.ark.tools.JsonValidator.expect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

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

  public void readJson(JsonNode node) {
    // Cluster and LocalProfile start

    JsonNode dinoClassNamesValue = node.path("remapDinoClassNames");

    if (expect(dinoClassNamesValue, "remapDinoClassNames", JsonNodeType.OBJECT)) {
      dinoClassNamesValue.fields().forEachRemaining(entry -> {
        if (expect(entry.getValue(), entry.getKey(), JsonNodeType.STRING)) {
          remapDinoClassName.put(entry.getKey(), entry.getValue().asText());
        }
      });
    }

    JsonNode itemArchetypesValue = node.path("remapItemArchetypes");

    if (expect(itemArchetypesValue, "remapItemArchetypes", JsonNodeType.OBJECT)) {
      itemArchetypesValue.fields().forEachRemaining(entry -> {
        if (expect(entry.getValue(), entry.getKey(), JsonNodeType.STRING)) {
          remapItemArchetypes.put(ArkName.from(entry.getKey()), ArkName.from(entry.getValue().asText()));
        }
      });
    }

    JsonNode removeItemsValue = node.path("removeItems");

    if (expect(removeItemsValue, "removeItems", JsonNodeType.ARRAY)) {
      for (JsonNode itemClass : removeItemsValue) {
        removeItems.add(ArkName.from(itemClass.asText()));
      }
    }

    JsonNode addItemsValue = node.path("addItems");

    if (expect(addItemsValue, "addItems", JsonNodeType.ARRAY)) {
      for (JsonNode item : addItemsValue) {
        addItems.add(new Item(item));
      }
    }
    
    // Cluster and LocalProfile end
    
    // Map start

    BiConsumer<String, Map<ArkName, List<Item>>> inventoryLoader = (fieldName, map) -> {
      JsonNode inventoriesValue = node.path(fieldName);

      if (expect(inventoriesValue, fieldName, JsonNodeType.OBJECT)) {
        inventoriesValue.fields().forEachRemaining(entry -> {
          if (expect(entry.getValue(), entry.getKey(), JsonNodeType.ARRAY)) {
            List<Item> items = new ArrayList<>();

            for (JsonNode item : entry.getValue()) {
              items.add(new Item(item));
            }

            map.put(ArkName.from(entry.getKey()), items);
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
