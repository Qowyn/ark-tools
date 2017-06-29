package qowyn.ark.tools;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.stream.JsonGenerator;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.arrays.ArkArrayStruct;
import qowyn.ark.tools.data.ArkItem;
import qowyn.ark.tools.data.AttributeNames;
import qowyn.ark.types.ArkByteValue;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.LocationData;
import qowyn.ark.types.ObjectReference;
import qowyn.ark.structs.StructPropertyList;

public class SharedWriters {

  private static void writeFloat(JsonGenerator generator, String name, float value) {
    if (Float.isFinite(value)) {
      generator.write(name, value);
    } else {
      generator.write(name, Float.toString(value));
    }
  }

  public static void writeCreatureInfo(JsonGenerator generator, GameObject creature, LatLonCalculator latLongCalculator, GameObjectContainer saveFile, boolean writeAllProperties) {
    writeCreatureInfo(generator, creature, latLongCalculator, saveFile, writeAllProperties, null);
  }

  public static void writeCreatureInfo(JsonGenerator generator, GameObject creature, LatLonCalculator latLongCalculator, GameObjectContainer saveFile, boolean writeAllProperties, String fieldName) {
    if (fieldName == null) {
      generator.writeStartObject();
    } else {
      generator.writeStartObject(fieldName);
    }

    LocationData ld = creature.getLocation();
    if (ld != null) {
      writeFloat(generator, "x", ld.getX());
      writeFloat(generator, "y", ld.getY());
      writeFloat(generator, "z", ld.getZ());
      if (latLongCalculator != null) {
        generator.write("lat", Math.round(latLongCalculator.calculateLat(ld.getY()) * 10.0) / 10.0);
        generator.write("lon", Math.round(latLongCalculator.calculateLon(ld.getX()) * 10.0) / 10.0);
      }
    }

    int dinoID1 = creature.findPropertyValue("DinoID1", Integer.class).orElse(0);
    int dinoID2 = creature.findPropertyValue("DinoID2", Integer.class).orElse(0);
    long id = (long) dinoID1 << Integer.SIZE | (dinoID2 & 0xFFFFFFFFL);
    generator.write("id", id);

    if (creature.findPropertyValue("TargetingTeam", Integer.class).orElse(0) >= 50000) {
      generator.write("tamed", true);
      generator.write("team", creature.findPropertyValue("TargetingTeam", Integer.class).orElse(0));
    } else if (writeAllProperties) {
      generator.write("tamed", false);
      generator.write("team", creature.findPropertyValue("TargetingTeam", Integer.class).orElse(0));
    }

    int playerId = creature.findPropertyValue("OwningPlayerID", Integer.class).orElse(0);
    if (playerId != 0) {
      generator.write("playerId", playerId);
    } else if (writeAllProperties) {
      generator.write("playerId", playerId);
    }

    if (creature.hasAnyProperty("bIsFemale")) {
      generator.write("female", true);
    } else if (writeAllProperties) {
      generator.write("female", false);
    }

    for (int i = 0; i < 6; i++) {
      ArkByteValue color = creature.getPropertyValue("ColorSetIndices", ArkByteValue.class, i);
      if (color != null) {
        generator.write("color" + i, Byte.toUnsignedInt(color.getByteValue()));
      } else if (writeAllProperties) {
        generator.write("color" + i, 0);
      }
    }

    creature.findPropertyValue("TamedAtTime", Double.class).ifPresent(tamedAtTime -> {
      generator.write("tamedAtTime", tamedAtTime);
      if (saveFile instanceof ArkSavegame) {
        generator.write("tamedTime", ((ArkSavegame) saveFile).getGameTime() - tamedAtTime);
      }
    });

    String tribeName = creature.getPropertyValue("TribeName", String.class);
    if (tribeName != null) {
      generator.write("tribe", tribeName);
    } else if (writeAllProperties) {
      generator.write("tribe", "");
    }

    String tamerName = creature.getPropertyValue("TamerString", String.class);
    if (tamerName != null) {
      generator.write("tamer", tamerName);
    } else if (writeAllProperties) {
      generator.write("tamer", "");
    }

    String ownerPlayerName = creature.getPropertyValue("OwningPlayerName", String.class);
    if (ownerPlayerName != null) {
      generator.write("ownerName", ownerPlayerName);
    } else if (writeAllProperties) {
      generator.write("ownerName", "");
    }

    String name = creature.getPropertyValue("TamedName", String.class);
    if (name != null) {
      generator.write("name", name);
    } else if (writeAllProperties) {
      generator.write("name", "");
    }

    String imprinter = creature.getPropertyValue("ImprinterName", String.class);
    if (imprinter != null) {
      generator.write("imprinter", imprinter);
    } else if (writeAllProperties) {
      generator.write("imprinter", "");
    }

    ArkArrayStruct ancestors = creature.getPropertyValue("DinoAncestors", ArkArrayStruct.class);
    if (ancestors != null) {
      ancestors.forEach((value) -> {
        StructPropertyList propertyList = (StructPropertyList)value;
        int fatherID1 = propertyList.getPropertyValue("MaleDinoID1", Integer.class);
        int fatherID2 = propertyList.getPropertyValue("MaleDinoID2", Integer.class);
        long fatherID = (long) fatherID1 << Integer.SIZE | (fatherID2 & 0xFFFFFFFFL);
        generator.write("maleAncestorId", fatherID);
        int motherID1 = propertyList.getPropertyValue("FemaleDinoID1", Integer.class);
        int motherID2 = propertyList.getPropertyValue("FemaleDinoID2", Integer.class);
        long motherID = (long) motherID1 << Integer.SIZE | (motherID2 & 0xFFFFFFFFL);
        generator.write("femaleAncestorId", motherID);
      });
    }

    GameObject status = creature.findPropertyValue("MyCharacterStatusComponent", ObjectReference.class).map(saveFile::getObject).orElse(null);

    if (status != null && status.getClassString().startsWith("DinoCharacterStatusComponent_")) {
      int baseLevel = status.findPropertyValue("BaseCharacterLevel", Integer.class).orElse(1);
      generator.write("baseLevel", baseLevel);

      if (baseLevel > 1 || writeAllProperties) {
        generator.writeStartObject("wildLevels");
        AttributeNames.forEach((index, attrName) -> {
          ArkByteValue attrProp = status.getPropertyValue("NumberOfLevelUpPointsApplied", ArkByteValue.class, index);
          if (attrProp != null) {
            generator.write(attrName, Byte.toUnsignedInt(attrProp.getByteValue()));
          } else if (writeAllProperties) {
            generator.write(attrName, 0);
          }
        });
        generator.writeEnd();
      }

      short extraLevel = status.findPropertyValue("ExtraCharacterLevel", Short.class).orElse((short) 0);
      if (extraLevel != 0) {
        generator.write("fullLevel", extraLevel + baseLevel);
      } else if (writeAllProperties) {
        generator.write("fullLevel", baseLevel);
      }

      if (status.hasAnyProperty("NumberOfLevelUpPointsAppliedTamed") || writeAllProperties) {
        generator.writeStartObject("tamedLevels");
        AttributeNames.forEach((index, attrName) -> {
          ArkByteValue attrProp = status.getPropertyValue("NumberOfLevelUpPointsAppliedTamed", ArkByteValue.class, index);
          if (attrProp != null) {
            generator.write(attrName, Byte.toUnsignedInt(attrProp.getByteValue()));
          } else if (writeAllProperties) {
            generator.write(attrName, 0);
          }
        });
        generator.writeEnd();
      }

      Float experience = status.getPropertyValue("ExperiencePoints", Float.class);
      if (experience != null) {
        generator.write("experience", experience);
      } else if (writeAllProperties) {
        generator.write("experience", 0);
      }

      Float tamedIneffectivenessModifier = status.getPropertyValue("TamedIneffectivenessModifier", Float.class);
      if (tamedIneffectivenessModifier != null) {
        generator.write("tamedEffectivenessModifier", 1 - tamedIneffectivenessModifier);
      } else if (writeAllProperties) {
        generator.write("tamedEffectivenessModifier", 0);
      }

      Float imprintingQuality = status.getPropertyValue("DinoImprintingQuality", Float.class);
      if (imprintingQuality != null) {
        generator.write("imprintingQuality", imprintingQuality);
      } else if (writeAllProperties) {
        generator.write("imprintingQuality", 0);
      }
    }

    generator.writeEnd();
  }

  public static void writeInventorySummary(JsonGenerator generator, List<ArkItem> items, String objName) {
    Map<ArkName, Integer> itemMap = new HashMap<>();

    for (ArkItem item : items) {
      itemMap.merge(item.className, item.quantity, Integer::sum);
    }

    generator.writeStartArray(objName);

    itemMap.entrySet().stream().sorted(comparing(Map.Entry::getValue, reverseOrder())).forEach(e -> {
      generator.writeStartObject();

      String name = e.getKey().toString();
      if (DataManager.hasItem(name)) {
        name = DataManager.getItem(name).getName();
      }

      generator.write("name", name);
      generator.write("count", e.getValue());

      generator.writeEnd();
    });

    generator.writeEnd();
  }

  public static void writeInventoryLong(JsonGenerator generator, List<ArkItem> items, String objName) {
    writeInventoryLong(generator, items, objName, false);
  }

  public static void writeInventoryLong(JsonGenerator generator, List<ArkItem> items, String objName, boolean blueprintStatus) {
    generator.writeStartArray(objName);

    items.sort(Comparator.comparing(item -> DataManager.hasItem(item.className.toString()) ? DataManager.getItem(item.className.toString()).getName() : item.className.toString()));
    for (ArkItem item : items) {
      generator.writeStartObject();

      String name = item.className.toString();
      if (DataManager.hasItem(name)) {
        name = DataManager.getItem(name).getName();
      }

      generator.write("name", name);

      if (blueprintStatus) {
        generator.write("isBlueprint", item.isBlueprint);
      }

      if (item.quantity > 1) {
        generator.write("quantity", item.quantity);
      }

      if (!item.customName.isEmpty()) {
        generator.write("customName", item.customName);
      }

      if (!item.customDescription.isEmpty()) {
        generator.write("customDescription", item.customDescription);
      }

      if (!item.isBlueprint && item.durability > 0.0f) {
        generator.write("durability", item.durability);
      }

      if (item.rating > 0.0f) {
        generator.write("rating", item.rating);
      }

      if (item.quality > 0) {
        generator.write("quality", item.quality);
      }

      if (item.itemStatValues[1] != 0) {
        generator.write("armorMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[1])) * 0.2f * 0.001f);
      }

      if (item.itemStatValues[2] != 0) {
        generator.write("durabilityMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[2])) * 0.25f * 0.001f);
      }

      if (item.itemStatValues[3] != 0) {
        generator.write("damageMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[3])) * 0.1f * 0.001f);
      }

      if (item.itemStatValues[5] != 0) {
        generator.write("hypoMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[5])) * 0.2f * 0.001f);
      }

      if (item.itemStatValues[7] != 0) {
        generator.write("hyperMultiplier", 1.0f + ((float) Short.toUnsignedInt(item.itemStatValues[7])) * 0.2f * 0.001f);
      }

      if (item.className.toString().contains("_Fertilized_")) {
        generator.writeStartObject("eggAttributes");

        for (int i = 0; i < item.eggLevelups.length; i++) {
          byte value = item.eggLevelups[i];
          if (value != 0) {
            generator.write(AttributeNames.get(i), Byte.toUnsignedInt(value));
          }
        }

        generator.writeEnd();

        generator.writeStartObject("eggColors");

        for (int i = 0; i < item.eggColors.length; i++) {
          byte value = item.eggColors[i];
          if (value != 0) {
            generator.write(Integer.toString(i), Byte.toUnsignedInt(value));
          }
        }

        generator.writeEnd();
      }

      generator.writeEnd();
    }
    generator.writeEnd();
  }

}
