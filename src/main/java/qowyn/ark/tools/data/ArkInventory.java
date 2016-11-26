package qowyn.ark.tools.data;

import java.util.ArrayList;
import java.util.List;

import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.types.ObjectReference;

public class ArkInventory {

  private List<ArkItem> inventoryItems = new ArrayList<>();

  private List<ArkItem> equippedItems = new ArrayList<>();

  public ArkInventory(GameObject inventory, GameObjectContainer container) {
    List<ObjectReference> inventoryItemReferences = inventory.getPropertyValue("InventoryItems", ArkArrayObjectReference.class);
    List<ObjectReference> equippedItemReferences = inventory.getPropertyValue("EquippedItems", ArkArrayObjectReference.class);

    if (inventoryItemReferences != null) {
      for (ObjectReference inventoryItem : inventoryItemReferences) {
        GameObject item = inventoryItem.getObject(container);
        if (item != null) {
          inventoryItems.add(new ArkItem(item));
        }
      }
    }

    if (equippedItemReferences != null) {
      for (ObjectReference equippedItem : equippedItemReferences) {
        GameObject item = equippedItem.getObject(container);
        if (item != null) {
          equippedItems.add(new ArkItem(item));
        }
      }
    }
  }

  public List<ArkItem> getInventoryItems() {
    return inventoryItems;
  }

  public List<ArkItem> getEquippedItems() {
    return equippedItems;
  }

}
