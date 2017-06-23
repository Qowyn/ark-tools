package qowyn.ark.tools.data;

import java.util.ArrayList;
import java.util.List;

import qowyn.ark.GameObject;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.types.ObjectReference;

public class Inventory {

  public List<Integer> inventoryItems = new ArrayList<>();

  public List<Integer> equippedItems = new ArrayList<>();

  public List<Integer> itemSlots = new ArrayList<>();

  public double lastInventoryRefreshTime;

  public Inventory(GameObject inventory) {

    List<ObjectReference> inventoryItemReferences = inventory.getPropertyValue("InventoryItems", ArkArrayObjectReference.class);
    if (inventoryItemReferences != null) {
      for (ObjectReference inventoryItem : inventoryItemReferences) {
        inventoryItems.add(inventoryItem.getObjectId());
      }
    }

    List<ObjectReference> equippedItemReferences = inventory.getPropertyValue("EquippedItems", ArkArrayObjectReference.class);
    if (equippedItemReferences != null) {
      for (ObjectReference equippedItem : equippedItemReferences) {
        equippedItems.add(equippedItem.getObjectId());
      }
    }

    List<ObjectReference> itemSlotReferences = inventory.getPropertyValue("ItemSlots", ArkArrayObjectReference.class);
    if (itemSlotReferences != null) {
      for (ObjectReference itemSlot : itemSlotReferences) {
        itemSlots.add(itemSlot.getObjectId());
      }
    }

    lastInventoryRefreshTime = inventory.findPropertyValue("LastInventoryRefreshTime", Double.class).orElse(0.0);
  }

}
