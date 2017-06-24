package qowyn.ark.tools.data;

import qowyn.ark.GameObject;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.types.ObjectReference;

public class Structure {

  public String id;

  public int myInventoryComponent;

  public boolean containerActivated;

  public int owningPlayerId;

  public String owningPlayerName;

  public int[] linkedStructures;

  public int placedOnFloorStructure;

  public String ownerName;

  public float maxHealth;

  public int targetingTeam;

  public Structure(GameObject structure) {
    id = structure.getNames().get(0).toString();

    myInventoryComponent = structure.findPropertyValue("MyInventoryComponent", ObjectReference.class).map(ObjectReference::getObjectId).orElse(-1);

    containerActivated = structure.findPropertyValue("bContainerActivated", Boolean.class).orElse(false);

    owningPlayerId = structure.findPropertyValue("OwningPlayerID", Integer.class).orElse(0);

    owningPlayerName = structure.findPropertyValue("OwningPlayerName", String.class).orElse("");

    ArkArrayObjectReference linkedStructuresReferences = structure.getPropertyValue("LinkedStructures", ArkArrayObjectReference.class);

    if (linkedStructuresReferences != null) {
      linkedStructures = new int[linkedStructuresReferences.size()];
      int index = 0;
      for (ObjectReference ref: linkedStructuresReferences) {
        linkedStructures[index++] = ref.getObjectId();
      }
    }

    placedOnFloorStructure = structure.findPropertyValue("PlacedOnFloorStructure", ObjectReference.class).map(ObjectReference::getObjectId).orElse(-1);

    ownerName = structure.findPropertyValue("TamedName", String.class).orElse("");

    maxHealth = structure.findPropertyValue("MaxHealth", Float.class).orElse(0.0f);

    targetingTeam = structure.findPropertyValue("TargetingTeam", Integer.class).orElse(0);
  }

}
