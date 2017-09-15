package qowyn.ark.tools.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonGenerator;

import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.tools.CreatureData;
import qowyn.ark.tools.DataManager;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.LocationData;
import qowyn.ark.types.ObjectReference;

public class Structure {

  public String id;

  public ArkName className;

  public String type;

  public LocationData location;

  public GameObject inventory;

  public boolean containerActivated;

  public int owningPlayerId;

  public String owningPlayerName;

  public int[] linkedStructures;

  public int placedOnFloorStructure;

  public String ownerName;

  public String boxName;

  public String bedName;

  public float maxHealth;

  public float health;

  public int targetingTeam;

  public Structure(GameObject structure, GameObjectContainer saveFile) {
    id = structure.getNames().get(0).toString();

    className = structure.getClassName();
    CreatureData structureData = DataManager.getStructure(className.toString());
    type = structureData != null ? structureData.getName() : className.toString();

    location = structure.getLocation();

    inventory = structure.findPropertyValue("MyInventoryComponent", ObjectReference.class).map(saveFile::getObject).orElse(null);

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

    ownerName = structure.findPropertyValue("OwnerName", String.class).orElse("");
    boxName = structure.findPropertyValue("BoxName", String.class).orElse("");
    bedName = structure.findPropertyValue("BedName", String.class).orElse("");

    maxHealth = structure.findPropertyValue("MaxHealth", Float.class).orElse(0.0f);

    health = structure.findPropertyValue("Health", Float.class).orElse(maxHealth);

    targetingTeam = structure.findPropertyValue("TargetingTeam", Integer.class).orElse(0);
  }

  public static final SortedMap<String, WriterFunction<Structure>> PROPERTIES = new TreeMap<>();

  static {
    /**
     * Structure Properties
     */
    PROPERTIES.put("id", (structure, generator, context, writeEmpty) -> {
      generator.writeStringField("id", structure.id);
    });
    PROPERTIES.put("type", (structure, generator, context, writeEmpty) -> {
      if (context instanceof DataCollector) {
        generator.writeStringField("type", structure.className.toString());
      } else {
        generator.writeStringField("type", structure.type);
      }
    });
    PROPERTIES.put("location", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || structure.location != null) {
        if (structure.location == null) {
          generator.writeNullField("location");
        } else {
          generator.writeObjectFieldStart("location");
          generator.writeNumberField("x", structure.location.getX());
          generator.writeNumberField("y", structure.location.getY());
          generator.writeNumberField("z", structure.location.getZ());
          if (context.getLatLonCalculator() != null) {
            generator.writeNumberField("lat", context.getLatLonCalculator().calculateLat(structure.location.getY()));
            generator.writeNumberField("lon", context.getLatLonCalculator().calculateLon(structure.location.getX()));
          }
          generator.writeEndObject();
        }
      }
    });
    PROPERTIES.put("myInventoryComponent", (structure, generator, context, writeEmpty) -> {
      if (structure.inventory != null) {
        generator.writeNumberField("myInventoryComponent", structure.inventory.getId());
      } else if (writeEmpty) {
        generator.writeNullField("myInventoryComponent");
      }
    });
    PROPERTIES.put("containerActivated", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || structure.containerActivated) {
        generator.writeBooleanField("containerActivated", structure.containerActivated);
      }
    });
    PROPERTIES.put("owningPlayerId", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || structure.owningPlayerId != 0) {
        generator.writeNumberField("owningPlayerId", structure.owningPlayerId);
      }
    });
    PROPERTIES.put("owningPlayerName", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || !structure.owningPlayerName.isEmpty()) {
        generator.writeStringField("owningPlayerName", structure.owningPlayerName);
      }
    });
    PROPERTIES.put("linkedStructures", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || structure.linkedStructures != null && structure.linkedStructures.length > 0) {
        if (structure.linkedStructures == null) {
          generator.writeNullField("linkedStructures");
        } else {
          generator.writeArrayFieldStart("linkedStructures");
          for (int linkedStructure: structure.linkedStructures) {
            generator.writeNumber(linkedStructure);
          }
          generator.writeEndArray();
        }
      }
    });
    PROPERTIES.put("placedOnFloorStructure", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || structure.placedOnFloorStructure != -1) {
        generator.writeNumberField("placedOnFloorStructure", structure.placedOnFloorStructure);
      }
    });
    PROPERTIES.put("ownerName", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || !structure.ownerName.isEmpty()) {
        generator.writeStringField("ownerName", structure.ownerName);
      }
    });
    PROPERTIES.put("boxName", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || !structure.boxName.isEmpty()) {
        generator.writeStringField("boxName", structure.boxName);
      }
    });
    PROPERTIES.put("bedName", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || !structure.bedName.isEmpty()) {
        generator.writeStringField("bedName", structure.bedName);
      }
    });
    PROPERTIES.put("maxHealth", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || structure.maxHealth != 0) {
        generator.writeNumberField("maxHealth", structure.maxHealth);
      }
    });
    PROPERTIES.put("health", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || structure.health != structure.maxHealth) {
        generator.writeNumberField("health", structure.health);
      }
    });
    PROPERTIES.put("targetingTeam", (structure, generator, context, writeEmpty) -> {
      if (writeEmpty || structure.targetingTeam != 0) {
        generator.writeNumberField("targetingTeam", structure.targetingTeam);
      }
    });
  }

  public static final List<WriterFunction<Structure>> PROPERTIES_LIST = new ArrayList<>(PROPERTIES.values());

  public void writeAllProperties(JsonGenerator generator, DataContext context, boolean writeEmpty) throws IOException {
    for (WriterFunction<Structure> writer: PROPERTIES_LIST) {
      writer.accept(this, generator, context, writeEmpty);
    }
  }

  public void writeInventory(JsonGenerator generator, DataContext context, boolean writeEmpty, boolean inventorySummary) throws IOException {
    if (this.inventory != null) {
      Inventory inventory = new Inventory(this.inventory);
      generator.writeFieldName("inventory");
      inventory.writeInventory(generator, context, writeEmpty, inventorySummary);
    }
  }

}
