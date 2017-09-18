package qowyn.ark.tools.modification;

import static qowyn.ark.tools.CommonFunctions.*;

import java.util.HashSet;
import java.util.Set;

import qowyn.ark.GameObject;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.properties.PropertyObject;
import qowyn.ark.tools.ObjectCollector;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.LocationData;
import qowyn.ark.types.ObjectReference;

public class DeleteOperation {

  public float minX;

  public float minY;

  public float minZ;

  public float maxX;

  public float maxY;

  public float maxZ;

  public int team;

  public int maxDeleteCount;

  public final Set<ArkName> classes = new HashSet<>();

  public boolean checkAndDecrease(ArkName className) {
    if (maxDeleteCount == 0 || !classes.contains(className)) {
      return false;
    } else if (maxDeleteCount > 0) {
      maxDeleteCount--;
    }
    return true;
  }

  public boolean canApplyTo(GameObject target) {
    if (target.getLocation() == null || maxDeleteCount == 0) {
      return false;
    }

    LocationData location = target.getLocation();
    if (location.getX() < minX || location.getX() > maxX || location.getY() < minY || location.getY() > maxY || location.getZ() < minZ || location.getZ() > maxZ) {
      return false;
    }

    int teamToTest = target.findPropertyValue("TargetingTeam", Integer.class).orElse(-1);
    if (team >= 0 && teamToTest != team) {
      return false;
    }

    return true;
  }

  public boolean apply(GameObject target, ObjectCollector objects) {
    if (checkAndDecrease(target.getClassName())) {
      if (isWeapon(target)) {
        target.findPropertyValue("AssociatedPrimalItem", ObjectReference.class).ifPresent(objects::remove);

        GameObject myPawn = target.findPropertyValue("MyPawn", ObjectReference.class).map(objects::get).orElse(null);
        if (myPawn != null) {
          myPawn.getTypedProperty("CurrentWeapon", PropertyObject.class).getValue().setObjectId(-1);
        }
      } else if (isDroppedItem(target)) {
        target.findPropertyValue("MyItem", ObjectReference.class).ifPresent(objects::remove);
      } else if (!target.getComponents().isEmpty()) {
        for (GameObject component: target.getComponents().values()) {
          if (isInventory(component)) {
            component.findPropertyValue("InventoryItems", ArkArrayObjectReference.class).ifPresent(refs -> refs.forEach(objects::remove));
            component.findPropertyValue("EquippedItems", ArkArrayObjectReference.class).ifPresent(refs -> refs.forEach(objects::remove));
            // deleting ItemSlots should not be necessary, since everything should be in InventoryItems, but let's do it anyway
            component.findPropertyValue("ItemSlots", ArkArrayObjectReference.class).ifPresent(refs -> refs.forEach(objects::remove));
          }

          objects.remove(component);
        }
      }

      objects.remove(target);

      return true;
    }

    if (isWeapon(target)) {
      GameObject item = target.findPropertyValue("AssociatedPrimalItem", ObjectReference.class).map(objects::get).orElse(null);
      if (item != null && checkAndDecrease(item.getClassName())) {
        objects.remove(item);
        objects.remove(target);
      }
    } else if (isDroppedItem(target)) {
      GameObject item = target.findPropertyValue("MyItem", ObjectReference.class).map(objects::get).orElse(null);
      if (item != null && checkAndDecrease(item.getClassName())) {
        objects.remove(item);
        objects.remove(target);
      }
    } else if (!target.getComponents().isEmpty()) {
      for (GameObject component: target.getComponents().values()) {
        if (!isInventory(component)) {
          continue;
        }

        checkAndRemoveItems(component.getPropertyValue("InventoryItems", ArkArrayObjectReference.class), objects);
        checkAndRemoveItems(component.getPropertyValue("EquippedItems", ArkArrayObjectReference.class), objects);
        // deleting ItemSlots should not be necessary, since everything should be in InventoryItems, but let's do it anyway
        checkAndRemoveItems(component.getPropertyValue("ItemSlots", ArkArrayObjectReference.class), objects);
      }
    }

    return false;
  }

  protected void checkAndRemoveItems(ArkArrayObjectReference itemsReferences, ObjectCollector objects) {
    if (itemsReferences == null) {
      return;
    }

    for (ObjectReference reference: itemsReferences) {
      GameObject item = objects.get(reference);

      if (item != null && checkAndDecrease(item.getClassName())) {
        objects.remove(item);
      }
    }
  }

  @Override
  public String toString() {
    return "DeleteOperation [minX=" + minX + ", minY=" + minY + ", minZ=" + minZ + ", maxX=" + maxX + ", maxY=" + maxY + ", maxZ=" + maxZ + ", team=" + team + ", maxDeleteCount=" + maxDeleteCount
        + ", classes=" + classes + "]";
  }

  public static DeleteOperation createSimple() {
    DeleteOperation simpleDelete = new DeleteOperation();
    simpleDelete.minX = Float.NEGATIVE_INFINITY;
    simpleDelete.minY = Float.NEGATIVE_INFINITY;
    simpleDelete.minZ = Float.NEGATIVE_INFINITY;
    simpleDelete.maxX = Float.POSITIVE_INFINITY;
    simpleDelete.maxY = Float.POSITIVE_INFINITY;
    simpleDelete.maxZ = Float.POSITIVE_INFINITY;
    simpleDelete.maxDeleteCount = -1;
    simpleDelete.team = -1;

    return simpleDelete;
  }
}
