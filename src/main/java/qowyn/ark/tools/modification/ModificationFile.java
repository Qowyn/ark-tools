package qowyn.ark.tools.modification;

import static qowyn.ark.tools.JsonValidator.expect;
import static qowyn.ark.tools.JsonValidator.expectArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

import qowyn.ark.tools.data.Item;
import qowyn.ark.types.ArkName;

public class ModificationFile {

  public final Map<String, String> remapDinoClassName = new HashMap<>();

  public final Map<ArkName, ArkName> remapItemArchetypes = new HashMap<>();

  public final List<DeleteOperation> deleteOperations = new ArrayList<>();

  public final List<Item> addItems = new ArrayList<>();

  public final Map<ArkName, List<Item>> replaceDefaultInventoriesMap = new HashMap<>();

  public final Map<ArkName, List<Item>> replaceInventoriesMap = new HashMap<>();

  public final Map<ArkName, List<Item>> addDefaultInventoriesMap = new HashMap<>();

  public final Map<ArkName, List<Item>> addInventoriesMap = new HashMap<>();

  public void readJson(JsonNode node) {
    // Cluster and LocalProfile start

    JsonNode dinoClassNamesNode = node.path("remapDinoClassNames");

    if (expect(dinoClassNamesNode, "remapDinoClassNames", JsonNodeType.OBJECT)) {
      dinoClassNamesNode.fields().forEachRemaining(entry -> {
        if (expect(entry.getValue(), entry.getKey(), JsonNodeType.STRING)) {
          remapDinoClassName.put(entry.getKey(), entry.getValue().asText());
        }
      });
    }

    JsonNode itemArchetypesNode = node.path("remapItemArchetypes");

    if (expect(itemArchetypesNode, "remapItemArchetypes", JsonNodeType.OBJECT)) {
      itemArchetypesNode.fields().forEachRemaining(entry -> {
        if (expect(entry.getValue(), entry.getKey(), JsonNodeType.STRING)) {
          remapItemArchetypes.put(ArkName.from(entry.getKey()), ArkName.from(entry.getValue().asText()));
        }
      });
    }

    JsonNode deletesNode = node.path("delete");
    DeleteOperation simpleDelete = DeleteOperation.createSimple();

    if (expect(deletesNode, "delete", JsonNodeType.ARRAY)) {
      for (JsonNode deleteNode : deletesNode) {
        if (deleteNode.isTextual()) {
          simpleDelete.classes.add(ArkName.from(deleteNode.asText()));
        } else if (deleteNode.isObject()) {
          DeleteOperation delete = new DeleteOperation();
          delete.minX = (float) deleteNode.path("minX").asDouble(Float.NEGATIVE_INFINITY);
          delete.minY = (float) deleteNode.path("minY").asDouble(Float.NEGATIVE_INFINITY);
          delete.minZ = (float) deleteNode.path("minZ").asDouble(Float.NEGATIVE_INFINITY);
          delete.maxX = (float) deleteNode.path("maxX").asDouble(Float.POSITIVE_INFINITY);
          delete.maxY = (float) deleteNode.path("maxY").asDouble(Float.POSITIVE_INFINITY);
          delete.maxZ = (float) deleteNode.path("maxZ").asDouble(Float.POSITIVE_INFINITY);

          delete.team = deleteNode.path("team").asInt(-1);
          delete.maxDeleteCount = deleteNode.path("maxDeleteCount").asInt(-1);

          for (JsonNode classesNode: deleteNode.path("classes")) {
            delete.classes.add(ArkName.from(classesNode.asText()));
          }

          deleteOperations.add(delete);
        } else {
          expectArray(deleteNode, "delete", JsonNodeType.STRING, JsonNodeType.OBJECT);
        }
      }
    }

    if (!simpleDelete.classes.isEmpty()) {
      deleteOperations.add(simpleDelete);
    }

    JsonNode addItemsNode = node.path("addItems");

    if (expect(addItemsNode, "addItems", JsonNodeType.ARRAY)) {
      for (JsonNode item : addItemsNode) {
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
