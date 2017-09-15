package qowyn.ark.tools.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonGenerator;

import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.tools.CreatureData;
import qowyn.ark.tools.DataManager;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.LocationData;
import qowyn.ark.types.ObjectReference;

public class DroppedItem {

  public ArkName className;

  public String type;

  public LocationData location;

  public GameObject myItem;

  public String droppedByName;

  public int targetingTeam;

  public double originalCreationTime;

  public float initialLifeSpan;

  public DroppedItem() {}

  public DroppedItem(GameObject droppedItem, GameObjectContainer saveFile) {
    className = droppedItem.getClassName();
    CreatureData structureData = DataManager.getStructure(droppedItem.getClassString());
    type = structureData != null ? structureData.getName() : droppedItem.getClassString();

    location = droppedItem.getLocation();

    myItem = droppedItem.findPropertyValue("MyItem", ObjectReference.class).map(saveFile::getObject).orElse(null);
    droppedByName = droppedItem.findPropertyValue("DroppedByName", String.class).orElse("");
    targetingTeam = droppedItem.findPropertyValue("TargetingTeam", Integer.class).orElse(0);
    originalCreationTime = droppedItem.findPropertyValue("OriginalCreationTime", Double.class).orElse(0.0);
    initialLifeSpan = droppedItem.findPropertyValue("InitialLifeSpan", Float.class).orElse(Float.POSITIVE_INFINITY);
  }

  public static final SortedMap<String, WriterFunction<DroppedItem>> PROPERTIES = new TreeMap<>();

  static {
    /**
     * Creature Properties
     */
    PROPERTIES.put("type", (droppedItem, generator, context, writeEmpty) -> {
      if (context instanceof DataCollector) {
        generator.writeStringField("type", droppedItem.className.toString());
      } else {
        generator.writeStringField("type", droppedItem.type);
      }
    });
    PROPERTIES.put("location", (droppedItem, generator, context, writeEmpty) -> {
      if (writeEmpty || droppedItem.location != null) {
        if (droppedItem.location == null) {
          generator.writeNullField("location");
        } else {
          generator.writeObjectFieldStart("location");
          generator.writeNumberField("x", droppedItem.location.getX());
          generator.writeNumberField("y", droppedItem.location.getY());
          generator.writeNumberField("z", droppedItem.location.getZ());
          if (context.getLatLonCalculator() != null) {
            generator.writeNumberField("lat", context.getLatLonCalculator().calculateLat(droppedItem.location.getY()));
            generator.writeNumberField("lon", context.getLatLonCalculator().calculateLon(droppedItem.location.getX()));
          }
          generator.writeEndObject();
        }
      }
    });
    PROPERTIES.put("myItem", (droppedItem, generator, context, writeEmpty) -> {
      if (droppedItem.myItem != null) {
        generator.writeNumberField("myItem", droppedItem.myItem.getId());
      } else if (writeEmpty) {
        generator.writeNullField("myItem");
      }
    });
    PROPERTIES.put("droppedByName", (droppedItem, generator, context, writeEmpty) -> {
      if (writeEmpty || !droppedItem.droppedByName.isEmpty()) {
        generator.writeStringField("droppedByName", droppedItem.droppedByName);
      }
    });
    PROPERTIES.put("targetingTeam", (droppedItem, generator, context, writeEmpty) -> {
      if (writeEmpty || droppedItem.targetingTeam != 0) {
        generator.writeNumberField("targetingTeam", droppedItem.targetingTeam);
      }
    });
    PROPERTIES.put("originalCreationTime", (droppedItem, generator, context, writeEmpty) -> {
      if (writeEmpty || droppedItem.originalCreationTime != 0.0) {
        generator.writeNumberField("originalCreationTime", droppedItem.originalCreationTime);
      }
    });
    PROPERTIES.put("initialLifeSpan", (droppedItem, generator, context, writeEmpty) -> {
      if (writeEmpty || droppedItem.initialLifeSpan != Float.POSITIVE_INFINITY) {
        generator.writeNumberField("initialLifeSpan", droppedItem.initialLifeSpan);
      }
    });
    PROPERTIES.put("lifeSpanLeft", (droppedItem, generator, context, writeEmpty) -> {
      if (context.getSavegame() != null && droppedItem.initialLifeSpan != Float.POSITIVE_INFINITY) {
        generator.writeNumberField("lifeSpanLeft", droppedItem.initialLifeSpan - (context.getSavegame().getGameTime() - droppedItem.originalCreationTime));
      } else if (writeEmpty) {
        if (droppedItem.initialLifeSpan != Float.POSITIVE_INFINITY) {
          generator.writeNumberField("lifeSpanLeft", Float.NaN);
        } else {
          generator.writeNumberField("lifeSpanLeft", Float.POSITIVE_INFINITY);
        }
      }
    });
  }

  public static final List<WriterFunction<DroppedItem>> PROPERTIES_LIST = new ArrayList<>(PROPERTIES.values());

  public void writeAllProperties(JsonGenerator generator, DataContext context, boolean writeEmpty) throws IOException {
    for (WriterFunction<DroppedItem> writer: PROPERTIES_LIST) {
      writer.accept(this, generator, context, writeEmpty);
    }
  }

  public void writeInventory(JsonGenerator generator, DataContext context, boolean writeEmpty, boolean inventorySummary) throws IOException {
    if (this.myItem != null) {
      Item item = new Item(myItem);

      generator.writeFieldName("item");
      if (inventorySummary) {
        generator.writeStartObject();

        generator.writeStringField("name", item.type);
        generator.writeNumberField("count", item.quantity);

        generator.writeEndObject();
      } else {
        generator.writeStartObject();

        item.writeAllProperties(generator, context, writeEmpty);

        generator.writeEndObject();
      }
    }
  }

}
