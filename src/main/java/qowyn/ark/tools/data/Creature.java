package qowyn.ark.tools.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonGenerator;

import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.arrays.ArkArrayStruct;
import qowyn.ark.structs.StructPropertyList;
import qowyn.ark.tools.CreatureData;
import qowyn.ark.tools.DataManager;
import qowyn.ark.types.ArkByteValue;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.LocationData;
import qowyn.ark.types.ObjectReference;

public class Creature {

  public static final int COLOR_SLOT_COUNT = 6;

  public ArkName className;

  public String type;

  public LocationData location;

  public long dinoId;

  public boolean tamed;

  public int targetingTeam;

  public int owningPlayerId;

  public boolean isFemale;

  public final byte colorSetIndices[] = new byte[COLOR_SLOT_COUNT];

  public double tamedAtTime;

  public String tribeName;

  public String tamerString;

  public String owningPlayerName;

  public String tamedName;

  public String imprinterName;

  public final ArrayList<AncestorLineEntry> femaleAncestors = new ArrayList<>();

  public final ArrayList<AncestorLineEntry> maleAncestors = new ArrayList<>();

  public int baseCharacterLevel;

  public final byte[] numberOfLevelUpPointsApplied = new byte[AttributeNames.size()];

  public short extraCharacterLevel;

  public final byte[] numberOfLevelUpPointsAppliedTamed = new byte[AttributeNames.size()];

  public boolean allowLevelUps;

  public float experiencePoints;

  public float dinoImprintingQuality;

  public float wildRandomScale;

  public boolean isWakingTame;

  public boolean isSleeping;

  public float requiredTameAffinity;

  public float currentTameAffinity;

  public float tamedIneffectivenessModifier;

  public int tamedFollowTarget;

  public int tamingTeamID;

  public String tamedOnServerName;

  public String uploadedFromServerName;

  public int tamedAggressionLevel;

  public float matingProgress;

  public double lastEnterStasisTime;

  public GameObject status;

  public GameObject inventory;

  public Creature(GameObject creature, GameObjectContainer container) {
    className = creature.getClassName();
    CreatureData creatureData = DataManager.getCreature(creature.getClassString());
    type = creatureData != null ? creatureData.getName() : creature.getClassString();

    location = creature.getLocation();
    
    int dinoID1 = creature.findPropertyValue("DinoID1", Integer.class).orElse(0);
    int dinoID2 = creature.findPropertyValue("DinoID2", Integer.class).orElse(0);
    dinoId = (long) dinoID1 << Integer.SIZE | (dinoID2 & 0xFFFFFFFFL);

    targetingTeam = creature.findPropertyValue("TargetingTeam", Integer.class).orElse(0);
    tamed = targetingTeam < 0 || targetingTeam >= 50000;

    owningPlayerId = creature.findPropertyValue("OwningPlayerID", Integer.class).orElse(0);

    isFemale = creature.findPropertyValue("bIsFemale", Boolean.class).orElse(false);

    for (int i = 0; i < 6; i++) {
      colorSetIndices[i] = creature.findPropertyValue("ColorSetIndices", ArkByteValue.class, i).map(ArkByteValue::getByteValue).orElse((byte) 0);
    }

    tamedAtTime = creature.findPropertyValue("TamedAtTime", Double.class).orElse(0.0);

    tribeName = creature.findPropertyValue("TribeName", String.class).orElse("");

    tamerString = creature.findPropertyValue("TamerString", String.class).orElse("");

    owningPlayerName = creature.findPropertyValue("OwningPlayerName", String.class).orElse("");

    tamedName = creature.findPropertyValue("TamedName", String.class).orElse("");

    imprinterName = creature.findPropertyValue("ImprinterName", String.class).orElse("");

    // Not all ancestors are saved. Only those ancestor information 
    // are available which are displayed ingame in the UI.
    creature.findPropertyValue("DinoAncestors", ArkArrayStruct.class).ifPresent(ancestors -> {
      // traverse female ancestor line
      ancestors.forEach((value) -> {
        StructPropertyList propertyList = (StructPropertyList)value;
        AncestorLineEntry entry = new AncestorLineEntry();

        entry.maleName = propertyList.findPropertyValue("MaleName", String.class).orElse("");
        int fatherID1 = propertyList.getPropertyValue("MaleDinoID1", Integer.class);
        int fatherID2 = propertyList.getPropertyValue("MaleDinoID2", Integer.class);
        entry.maleId = (long) fatherID1 << Integer.SIZE | (fatherID2 & 0xFFFFFFFFL);

        entry.femaleName = propertyList.findPropertyValue("FemaleName", String.class).orElse("");
        int motherID1 = propertyList.getPropertyValue("FemaleDinoID1", Integer.class);
        int motherID2 = propertyList.getPropertyValue("FemaleDinoID2", Integer.class);
        entry.femaleId = (long) motherID1 << Integer.SIZE | (motherID2 & 0xFFFFFFFFL);

        femaleAncestors.add(entry);
      });
    });

    creature.findPropertyValue("DinoAncestorsMale", ArkArrayStruct.class).ifPresent(ancestors -> {
      // traverse male ancestor line
      ancestors.forEach((value) -> {
        StructPropertyList propertyList = (StructPropertyList)value;
        AncestorLineEntry entry = new AncestorLineEntry();

        entry.maleName = propertyList.findPropertyValue("MaleName", String.class).orElse("");
        int fatherID1 = propertyList.getPropertyValue("MaleDinoID1", Integer.class);
        int fatherID2 = propertyList.getPropertyValue("MaleDinoID2", Integer.class);
        entry.maleId = (long) fatherID1 << Integer.SIZE | (fatherID2 & 0xFFFFFFFFL);

        entry.femaleName = propertyList.findPropertyValue("FemaleName", String.class).orElse("");
        int motherID1 = propertyList.getPropertyValue("FemaleDinoID1", Integer.class);
        int motherID2 = propertyList.getPropertyValue("FemaleDinoID2", Integer.class);
        entry.femaleId = (long) motherID1 << Integer.SIZE | (motherID2 & 0xFFFFFFFFL);

        maleAncestors.add(entry);
      });
    });

    wildRandomScale = creature.findPropertyValue("WildRandomScale", Float.class).orElse(1.0f);

    isWakingTame = creature.findPropertyValue("bIsWakingTame", Boolean.class).orElse(false);

    isSleeping = creature.findPropertyValue("bIsSleeping", Boolean.class).orElse(false);

    requiredTameAffinity = creature.findPropertyValue("RequiredTameAffinity", Float.class).orElse(0.0f);

    currentTameAffinity = creature.findPropertyValue("CurrentTameAffinity", Float.class).orElse(0.0f);

    tamedIneffectivenessModifier = creature.findPropertyValue("TameIneffectivenessModifier", Float.class).orElse(0.0f);

    tamedFollowTarget = creature.findPropertyValue("TamedFollowTarget", ObjectReference.class).map(ObjectReference::getObjectId).orElse(-1);

    tamingTeamID = creature.findPropertyValue("TamingTeamID", Integer.class).orElse(0);

    tamedOnServerName = creature.findPropertyValue("TamedOnServerName", String.class).orElse("");

    uploadedFromServerName = creature.findPropertyValue("UploadedFromServerName", String.class).orElse("");

    tamedAggressionLevel = creature.findPropertyValue("TamedAggressionLevel", Integer.class).orElse(0);

    matingProgress = creature.findPropertyValue("MatingProgress", Float.class).orElse(0.0f);

    lastEnterStasisTime = creature.findPropertyValue("LastEnterStasisTime", Double.class).orElse(0.0);

    status = creature.findPropertyValue("MyCharacterStatusComponent", ObjectReference.class).map(container::getObject).orElse(null);

    inventory = creature.findPropertyValue("MyInventoryComponent", ObjectReference.class).map(container::getObject).orElse(null);

    if (status != null && status.getClassString().startsWith("DinoCharacterStatusComponent_")) {
      baseCharacterLevel = status.findPropertyValue("BaseCharacterLevel", Integer.class).orElse(1);

      for (int index = 0; index < AttributeNames.size(); index++) {
        numberOfLevelUpPointsApplied[index] = status.findPropertyValue("NumberOfLevelUpPointsApplied", ArkByteValue.class, index).map(ArkByteValue::getByteValue).orElse((byte) 0);
      }

      extraCharacterLevel = status.findPropertyValue("ExtraCharacterLevel", Short.class).orElse((short) 0);

      for (int index = 0; index < AttributeNames.size(); index++) {
        numberOfLevelUpPointsAppliedTamed[index] = status.findPropertyValue("NumberOfLevelUpPointsAppliedTamed", ArkByteValue.class, index).map(ArkByteValue::getByteValue).orElse((byte) 0);
      }

      allowLevelUps = status.findPropertyValue("bAllowLevelUps", Boolean.class).orElse(false);

      experiencePoints = status.findPropertyValue("ExperiencePoints", Float.class).orElse(0.0f);

      dinoImprintingQuality = status.findPropertyValue("DinoImprintingQuality", Float.class).orElse(0.0f);

      tamedIneffectivenessModifier = status.findPropertyValue("TamedIneffectivenessModifier", Float.class).orElse(tamedIneffectivenessModifier);
    }
  }

  public static class AncestorLineEntry {

    public String maleName;

    public long maleId;

    public String femaleName;

    public long femaleId;

  }

  public static final SortedMap<String, WriterFunction<Creature>> PROPERTIES = new TreeMap<>();

  static {
    /**
     * Creature Properties
     */
    PROPERTIES.put("type", (creature, generator, context, writeEmpty) -> {
      if (context instanceof DataCollector) {
        generator.writeStringField("type", creature.className.toString());
      } else {
        generator.writeStringField("type", creature.type);
      }
    });
    PROPERTIES.put("location", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.location != null) {
        if (creature.location == null) {
          generator.writeNullField("location");
        } else {
          generator.writeObjectFieldStart("location");
          generator.writeNumberField("x", creature.location.getX());
          generator.writeNumberField("y", creature.location.getY());
          generator.writeNumberField("z", creature.location.getZ());
          if (context.getLatLonCalculator() != null) {
            generator.writeNumberField("lat", context.getLatLonCalculator().calculateLat(creature.location.getY()));
            generator.writeNumberField("lon", context.getLatLonCalculator().calculateLon(creature.location.getX()));
          }
          generator.writeEndObject();
        }
      }
    });
    PROPERTIES.put("myInventoryComponent", (creature, generator, context, writeEmpty) -> {
      if (creature.inventory != null) {
        generator.writeNumberField("myInventoryComponent", creature.inventory.getId());
      } else if (writeEmpty) {
        generator.writeNullField("myInventoryComponent");
      }
    });
    PROPERTIES.put("id", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.dinoId != 0) {
        generator.writeNumberField("id", creature.dinoId);
      }
    });
    PROPERTIES.put("tamed", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.tamed) {
        generator.writeBooleanField("tamed", creature.tamed);
      }
    });
    PROPERTIES.put("team", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.targetingTeam != 0) {
        generator.writeNumberField("team", creature.targetingTeam);
      }
    });
    PROPERTIES.put("playerId", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.owningPlayerId != 0) {
        generator.writeNumberField("playerId", creature.owningPlayerId);
      }
    });
    PROPERTIES.put("female", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.isFemale) {
        generator.writeBooleanField("female", creature.isFemale);
      }
    });
    PROPERTIES.put("colorSetIndices", (creature, generator, context, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeObjectFieldStart("colorSetIndices");
      }
      for (int index = 0; index < creature.colorSetIndices.length; index++) {
        if (writeEmpty || creature.colorSetIndices[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeObjectFieldStart("colorSetIndices");
          }
          generator.writeNumberField(Integer.toString(index), Byte.toUnsignedInt(creature.colorSetIndices[index]));
        }
      }
      if (!empty) {
        generator.writeEndObject();
      }
    });
    PROPERTIES.put("femaleAncestors", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || !creature.femaleAncestors.isEmpty()) {
        generator.writeArrayFieldStart("femaleAncestors");
        for (AncestorLineEntry entry: creature.femaleAncestors) {
          generator.writeStartObject();

          generator.writeStringField("maleName", entry.maleName);
          generator.writeNumberField("maleId", entry.maleId);
          generator.writeStringField("femaleName", entry.femaleName);
          generator.writeNumberField("femaleId", entry.femaleId);

          generator.writeEndObject();
        }
        generator.writeEndArray();
      }
    });
    PROPERTIES.put("maleAncestors", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || !creature.maleAncestors.isEmpty()) {
        generator.writeArrayFieldStart("maleAncestors");
        for (AncestorLineEntry entry: creature.maleAncestors) {
          generator.writeStartObject();

          generator.writeStringField("maleName", entry.maleName);
          generator.writeNumberField("maleId", entry.maleId);
          generator.writeStringField("femaleName", entry.femaleName);
          generator.writeNumberField("femaleId", entry.femaleId);

          generator.writeEndObject();
        }
        generator.writeEndArray();
      }
    });
    PROPERTIES.put("tamedAtTime", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.tamedAtTime != 0.0) {
        generator.writeNumberField("tamedAtTime", creature.tamedAtTime);
      }
    });
    PROPERTIES.put("tamedTime", (creature, generator, context, writeEmpty) -> {
      if (context.getSavegame() != null && creature.tamedAtTime != 0.0) {
        generator.writeNumberField("tamedTime", context.getSavegame().getGameTime() - creature.tamedAtTime);
      } else if (writeEmpty) {
        generator.writeNullField("tamedTime");
      }
    });
    PROPERTIES.put("tribe", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || !creature.tribeName.isEmpty()) {
        generator.writeStringField("tribe", creature.tribeName);
      }
    });
    PROPERTIES.put("tamer", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || !creature.tamerString.isEmpty()) {
        generator.writeStringField("tamer", creature.tamerString);
      }
    });
    PROPERTIES.put("ownerName", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || !creature.owningPlayerName.isEmpty()) {
        generator.writeStringField("ownerName", creature.owningPlayerName);
      }
    });
    PROPERTIES.put("name", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || !creature.tamedName.isEmpty()) {
        generator.writeStringField("name", creature.tamedName);
      }
    });
    PROPERTIES.put("imprinter", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || !creature.imprinterName.isEmpty()) {
        generator.writeStringField("imprinter", creature.imprinterName);
      }
    });
    PROPERTIES.put("baseLevel", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.baseCharacterLevel != 0) {
        generator.writeNumberField("baseLevel", creature.baseCharacterLevel);
      }
    });
    PROPERTIES.put("wildLevels", (creature, generator, context, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeObjectFieldStart("wildLevels");
      }
      for (int index = 0; index < creature.numberOfLevelUpPointsApplied.length; index++) {
        if (writeEmpty || creature.numberOfLevelUpPointsApplied[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeObjectFieldStart("wildLevels");
          }
          generator.writeNumberField(AttributeNames.get(index), Byte.toUnsignedInt(creature.numberOfLevelUpPointsApplied[index]));
        }
      }
      if (!empty) {
        generator.writeEndObject();
      }
    });
    PROPERTIES.put("extraLevel", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.extraCharacterLevel != 0) {
        generator.writeNumberField("extraLevel", creature.extraCharacterLevel);
      }
    });
    PROPERTIES.put("tamedLevels", (creature, generator, context, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeObjectFieldStart("tamedLevels");
      }
      for (int index = 0; index < creature.numberOfLevelUpPointsAppliedTamed.length; index++) {
        if (writeEmpty || creature.numberOfLevelUpPointsAppliedTamed[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeObjectFieldStart("tamedLevels");
          }
          generator.writeNumberField(AttributeNames.get(index), Byte.toUnsignedInt(creature.numberOfLevelUpPointsAppliedTamed[index]));
        }
      }
      if (!empty) {
        generator.writeEndObject();
      }
    });
    PROPERTIES.put("allowLevelUps", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.allowLevelUps) {
        generator.writeBooleanField("allowLevelUps", creature.allowLevelUps);
      }
    });
    PROPERTIES.put("experience", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.experiencePoints != 0.0f) {
        generator.writeNumberField("experience", creature.experiencePoints);
      }
    });
    PROPERTIES.put("imprintingQuality", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.dinoImprintingQuality != 0.0f) {
        generator.writeNumberField("imprintingQuality", creature.dinoImprintingQuality);
      }
    });
    PROPERTIES.put("wildRandomScale", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.wildRandomScale != 1.0f) {
        generator.writeNumberField("wildRandomScale", creature.wildRandomScale);
      }
    });
    PROPERTIES.put("isWakingTame", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.isWakingTame) {
        generator.writeBooleanField("isWakingTame", creature.isWakingTame);
      }
    });
    PROPERTIES.put("isSleeping", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.isSleeping) {
        generator.writeBooleanField("isSleeping", creature.isSleeping);
      }
    });
    PROPERTIES.put("requiredTameAffinity", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.requiredTameAffinity != 0.0f) {
        generator.writeNumberField("requiredTameAffinity", creature.requiredTameAffinity);
      }
    });
    PROPERTIES.put("currentTameAffinity", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.currentTameAffinity != 0.0f) {
        generator.writeNumberField("currentTameAffinity", creature.currentTameAffinity);
      }
    });
    PROPERTIES.put("tamingEffectivness", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.tamedIneffectivenessModifier != 1.0f) {
        generator.writeNumberField("tamingEffectivness", 1.0f - creature.tamedIneffectivenessModifier);
      }
    });
    PROPERTIES.put("tamedFollowTarget", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.tamedFollowTarget != -1) {
        generator.writeNumberField("tamedFollowTarget", creature.tamedFollowTarget);
      }
    });
    PROPERTIES.put("tamingTeamID", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.tamingTeamID != 0) {
        generator.writeNumberField("tamingTeamID", creature.tamingTeamID);
      }
    });
    PROPERTIES.put("tamedOnServerName", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || !creature.tamedOnServerName.isEmpty()) {
        generator.writeStringField("tamedOnServerName", creature.tamedOnServerName);
      }
    });
    PROPERTIES.put("uploadedFromServerName", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || !creature.uploadedFromServerName.isEmpty()) {
        generator.writeStringField("uploadedFromServerName", creature.uploadedFromServerName);
      }
    });
    PROPERTIES.put("tamedAggressionLevel", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.tamedAggressionLevel != 0) {
        generator.writeNumberField("tamedAggressionLevel", creature.tamedAggressionLevel);
      }
    });
    PROPERTIES.put("matingProgress", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.matingProgress != 0.0f) {
        generator.writeNumberField("matingProgress", creature.matingProgress);
      }
    });
    PROPERTIES.put("lastEnterStasisTime", (creature, generator, context, writeEmpty) -> {
      if (writeEmpty || creature.lastEnterStasisTime != 0.0) {
        generator.writeNumberField("lastEnterStasisTime", creature.lastEnterStasisTime);
      }
    });
  }

  public static final List<WriterFunction<Creature>> PROPERTIES_LIST = new ArrayList<>(PROPERTIES.values());

  public void writeAllProperties(JsonGenerator generator, DataContext context, boolean writeEmpty) throws IOException {
    for (WriterFunction<Creature> writer: PROPERTIES_LIST) {
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
