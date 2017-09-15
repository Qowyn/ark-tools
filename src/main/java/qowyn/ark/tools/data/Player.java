package qowyn.ark.tools.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonGenerator;

import qowyn.ark.ArkProfile;
import qowyn.ark.GameObject;
import qowyn.ark.PropertyContainer;
import qowyn.ark.ReadingOptions;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.structs.StructLinearColor;
import qowyn.ark.structs.StructPropertyList;
import qowyn.ark.structs.StructUniqueNetIdRepl;
import qowyn.ark.tools.CommonFunctions;
import qowyn.ark.types.ArkByteValue;
import qowyn.ark.types.LocationData;
import qowyn.ark.types.ObjectReference;

public class Player {

  public int savedPlayerDataVersion;

  /**
   * Player ID
   */
  public long playerDataId;

  /**
   * Steam ID
   */
  public StructUniqueNetIdRepl uniqueId;

  public String savedNetworkAddress;

  public String playerName;

  public int tribeId;

  public int playerDataVersion;

  public int spawnDayNumber;

  public float spawnDayTime;

  public boolean isFemale;

  public final StructLinearColor[] bodyColors = new StructLinearColor[PlayerBodyColorRegions.size()];

  public StructLinearColor overrideHeadHairColor;

  public StructLinearColor overrideFacialHairColor;

  public byte facialHairIndex;

  public byte headHairIndex;

  public String playerCharacterName;

  public final float[] rawBoneModifiers = new float[PlayerBoneModifierNames.size()];

  public int playerSpawnRegionIndex;

  public int totalEngramPoints;

  public final List<String> engramBlueprints = new ArrayList<>();

  public final byte[] numberOfLevelUpPointsApplied = new byte[AttributeNames.size()];

  public float percentageOfHeadHairGrowth;

  public float percentageOfFacialHairGrowth;

  public GameObject inventory;

  public int inventoryId = -1;

  public LocationData location;

  public int characterLevel;

  public float experiencePoints;

  public float lastHypothermalCharacterInsulationValue;

  public float lastHyperthermalCharacterInsulationValue;

  public final float[] currentStatusValues = new float[AttributeNames.size()];

  public Player(Path path, DataContext context) throws IOException {
    this(path, context, ReadingOptions.create());
  }

  public Player(Path path, DataContext context, ReadingOptions ro) throws IOException {
    ArkProfile profile = new ArkProfile(path, ro);

    savedPlayerDataVersion = profile.findPropertyValue("SavedPlayerDataVersion", Integer.class).orElse(0);

    StructPropertyList myData = profile.getPropertyValue("MyData", StructPropertyList.class);

    playerDataId = myData.getPropertyValue("PlayerDataID", Long.class);
    uniqueId = myData.getPropertyValue("UniqueID", StructUniqueNetIdRepl.class);
    savedNetworkAddress = myData.getPropertyValue("SavedNetworkAddress", String.class);
    playerName = myData.getPropertyValue("PlayerName", String.class);
    tribeId = myData.findPropertyValue("TribeID", Integer.class).orElse(0);
    playerDataVersion = myData.findPropertyValue("PlayerDataVersion", Integer.class).orElse(0);
    spawnDayNumber = myData.findPropertyValue("SpawnDayNumber", Integer.class).orElse(0);
    spawnDayTime = myData.findPropertyValue("SpawnDayTime", Float.class).orElse(0.0f);

    PropertyContainer characterConfig = myData.getPropertyValue("MyPlayerCharacterConfig", PropertyContainer.class);

    // Character data

    isFemale = characterConfig.findPropertyValue("bIsFemale", Boolean.class).orElse(false);
    for (int i = 0; i < PlayerBodyColorRegions.size(); i++) {
      bodyColors[i] = characterConfig.getPropertyValue("BodyColors", StructLinearColor.class, i);
    }
    overrideHeadHairColor = characterConfig.getPropertyValue("OverrideHeadHairColor", StructLinearColor.class);
    overrideFacialHairColor = characterConfig.getPropertyValue("OverrideFacialHairColor", StructLinearColor.class);
    facialHairIndex = characterConfig.findPropertyValue("FacialHairIndex", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    headHairIndex = characterConfig.findPropertyValue("HeadHairIndex", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    playerCharacterName = characterConfig.findPropertyValue("PlayerCharacterName", String.class).orElse("");
    for (int i = 0; i < PlayerBoneModifierNames.size(); i++) {
      rawBoneModifiers[i] = characterConfig.findPropertyValue("RawBoneModifiers", Float.class, i).orElse(0.0f);
    }

    PropertyContainer characterStats = myData.getPropertyValue("MyPersistentCharacterStats", PropertyContainer.class);

    characterLevel = characterStats.findPropertyValue("CharacterStatusComponent_ExtraCharacterLevel", Short.class).orElse((short) 0) + 1;
    experiencePoints = characterStats.findPropertyValue("CharacterStatusComponent_ExperiencePoints", Float.class).orElse(0.0f);
    totalEngramPoints = characterStats.findPropertyValue("PlayerState_TotalEngramPoints", Integer.class).orElse(0);

    List<ObjectReference> learnedEngrams = characterStats.getPropertyValue("PlayerState_EngramBlueprints", ArkArrayObjectReference.class);

    if (learnedEngrams != null) {
      for (ObjectReference reference : learnedEngrams) {
        engramBlueprints.add(reference.getObjectString().toString());
      }
    }

    for (int i = 0; i < AttributeNames.size(); i++) {
      numberOfLevelUpPointsApplied[i] = characterStats.findPropertyValue("CharacterStatusComponent_NumberOfLevelUpPointsApplied", ArkByteValue.class, i)
          .map(ArkByteValue::getByteValue).orElse((byte) 0);
    }

    percentageOfHeadHairGrowth = characterStats.findPropertyValue("PercentageOfHeadHairGrowth", Float.class).orElse(0.0f);
    percentageOfFacialHairGrowth = characterStats.findPropertyValue("PercentageOfFacialHairGrowth", Float.class).orElse(0.0f);

    if (context.getObjectContainer() == null) {
      return;
    }

    GameObject player = null;
    for (GameObject object : context.getObjectContainer().getObjects()) {
      Long linkedPlayerDataId = object.getPropertyValue("LinkedPlayerDataID", Long.class);
      if (linkedPlayerDataId != null && linkedPlayerDataId == playerDataId) {
        player = object;
        break;
      }
    }

    if (player == null) {
      return;
    }

    GameObject playerCharacterStatus = null;
    for (GameObject object : context.getObjectContainer().getObjects()) {
      if (object.getNames().size() == 2 && object.getNames().get(0).toString().equals("PlayerCharacterStatus") && object.getNames().get(1) == player.getNames().get(0)) {
        playerCharacterStatus = object;
        break;
      }
    }

    inventory = player.findPropertyValue("MyInventoryComponent", ObjectReference.class).map(context.getObjectContainer()::getObject).orElse(null);
    inventoryId = inventory != null ? inventory.getId() : -1;
    location = player.getLocation();

    if (playerCharacterStatus != null) {
      lastHypothermalCharacterInsulationValue = playerCharacterStatus.findPropertyValue("LastHypothermalCharacterInsulationValue", Float.class).orElse(0.0f);
      lastHyperthermalCharacterInsulationValue = playerCharacterStatus.findPropertyValue("LastHyperthermalCharacterInsulationValue", Float.class).orElse(0.0f);

      for (int index = 0; index < AttributeNames.size(); index++) {
        currentStatusValues[index] = playerCharacterStatus.findPropertyValue("CurrentStatusValues", Float.class, index).orElse(0.0f);
      }
    }
  }

  public static final SortedMap<String, WriterFunction<Player>> PROPERTIES = new TreeMap<>();

  static {
    PROPERTIES.put("id", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.playerDataId != 0) {
        generator.writeNumberField("id", player.playerDataId);
      }
    });
    /**
     * Player Properties
     */
    PROPERTIES.put("steamId", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.uniqueId != null) {
        if (player.uniqueId == null) {
          generator.writeNullField("steamId");
        } else {
          generator.writeStringField("steamId", player.uniqueId.getNetId());
        }
      }
    });
    PROPERTIES.put("savedNetworkAddress", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.savedNetworkAddress != null) {
        if (player.savedNetworkAddress == null) {
          generator.writeNullField("savedNetworkAddress");
        } else {
          generator.writeStringField("savedNetworkAddress", player.savedNetworkAddress);
        }
      }
    });
    PROPERTIES.put("playerName", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.playerName != null) {
        if (player.playerName == null) {
          generator.writeNullField("playerName");
        } else {
          generator.writeStringField("playerName", player.playerName);
        }
      }
    });
    PROPERTIES.put("tribeId", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.tribeId != 0) {
        generator.writeNumberField("tribeId", player.tribeId);
      }
    });
    PROPERTIES.put("playerDataVersion", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.playerDataVersion != 0) {
        generator.writeNumberField("playerDataVersion", player.playerDataVersion);
      }
    });
    PROPERTIES.put("spawnDayNumber", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.spawnDayNumber != 0) {
        generator.writeNumberField("spawnDayNumber", player.spawnDayNumber);
      }
    });
    PROPERTIES.put("spawnDayTime", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.spawnDayTime != 0.0f) {
        generator.writeNumberField("spawnDayTime", player.spawnDayTime);
      }
    });
    PROPERTIES.put("isFemale", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.isFemale) {
        generator.writeBooleanField("isFemale", player.isFemale);
      }
    });
    PROPERTIES.put("bodyColors", (player, generator, context, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeObjectFieldStart("bodyColors");
      }
      for (int index = 0; index < player.bodyColors.length; index++) {
        if (writeEmpty || player.bodyColors[index] != null) {
          if (empty) {
            empty = false;
            generator.writeObjectFieldStart("bodyColors");
          }
          if (player.bodyColors[index] == null) {
            generator.writeNullField(PlayerBodyColorRegions.get(index));
          } else {
            generator.writeStringField(PlayerBodyColorRegions.get(index), CommonFunctions.getRGBA(player.bodyColors[index]));
          }
        }
      }
      if (!empty) {
        generator.writeEndObject();
      }
    });
    PROPERTIES.put("overrideHeadHairColor", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.overrideHeadHairColor != null) {
        if (player.overrideHeadHairColor == null) {
          generator.writeNullField("overrideHeadHairColor");
        } else {
          generator.writeStringField("overrideHeadHairColor", CommonFunctions.getRGBA(player.overrideHeadHairColor));
        }
      }
    });
    PROPERTIES.put("overrideFacialHairColor", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.overrideFacialHairColor != null) {
        if (player.overrideFacialHairColor == null) {
          generator.writeNullField("overrideFacialHairColor");
        } else {
          generator.writeStringField("overrideFacialHairColor", CommonFunctions.getRGBA(player.overrideFacialHairColor));
        }
      }
    });
    PROPERTIES.put("headHairIndex", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.headHairIndex != 0) {
        generator.writeNumberField("headHairIndex", Byte.toUnsignedInt(player.headHairIndex));
      }
    });
    PROPERTIES.put("facialHairIndex", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.facialHairIndex != 0) {
        generator.writeNumberField("facialHairIndex", Byte.toUnsignedInt(player.facialHairIndex));
      }
    });
    PROPERTIES.put("playerCharacterName", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || !player.playerCharacterName.isEmpty()) {
        generator.writeStringField("playerCharacterName", player.playerCharacterName);
      }
    });
    PROPERTIES.put("rawBoneModifiers", (player, generator, context, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeObjectFieldStart("rawBoneModifiers");
      }
      for (int index = 0; index < player.rawBoneModifiers.length; index++) {
        if (writeEmpty || player.rawBoneModifiers[index] != 0.0f) {
          if (empty) {
            empty = false;
            generator.writeObjectFieldStart("rawBoneModifiers");
          }
          generator.writeNumberField(PlayerBoneModifierNames.get(index), player.rawBoneModifiers[index]);
        }
      }
      if (!empty) {
        generator.writeEndObject();
      }
    });
    PROPERTIES.put("playerSpawnRegionIndex", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.playerSpawnRegionIndex != 0) {
        generator.writeNumberField("playerSpawnRegionIndex", player.playerSpawnRegionIndex);
      }
    });
    PROPERTIES.put("totalEngramPoints", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.totalEngramPoints != 0) {
        generator.writeNumberField("totalEngramPoints", player.totalEngramPoints);
      }
    });
    PROPERTIES.put("engramBlueprints", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || !player.engramBlueprints.isEmpty()) {
        generator.writeArrayFieldStart("engramBlueprints");
        for (String engramBlueprint : player.engramBlueprints) {
          generator.writeString(engramBlueprint);
        }
        generator.writeEndArray();
      }
    });
    PROPERTIES.put("numberOfLevelUpPointsApplied", (player, generator, context, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeObjectFieldStart("numberOfLevelUpPointsApplied");
      }
      for (int index = 0; index < player.numberOfLevelUpPointsApplied.length; index++) {
        if (writeEmpty || player.numberOfLevelUpPointsApplied[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeObjectFieldStart("numberOfLevelUpPointsApplied");
          }
          generator.writeNumberField(AttributeNames.get(index), Byte.toUnsignedInt(player.numberOfLevelUpPointsApplied[index]));
        }
      }
      if (!empty) {
        generator.writeEndObject();
      }
    });
    PROPERTIES.put("currentStatusValues", (player, generator, context, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeObjectFieldStart("currentStatusValues");
      }
      for (int index = 0; index < player.currentStatusValues.length; index++) {
        if (writeEmpty || player.currentStatusValues[index] != 0.0f) {
          if (empty) {
            empty = false;
            generator.writeObjectFieldStart("currentStatusValues");
          }
          generator.writeNumberField(AttributeNames.get(index), player.currentStatusValues[index]);
        }
      }
      if (!empty) {
        generator.writeEndObject();
      }
    });
    PROPERTIES.put("percentageOfHeadHairGrowth", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.percentageOfHeadHairGrowth != 0.0f) {
        generator.writeNumberField("percentageOfHeadHairGrowth", player.percentageOfHeadHairGrowth);
      }
    });
    PROPERTIES.put("percentageOfFacialHairGrowth", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.percentageOfFacialHairGrowth != 0.0f) {
        generator.writeNumberField("percentageOfFacialHairGrowth", player.percentageOfFacialHairGrowth);
      }
    });
    PROPERTIES.put("inventoryId", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.inventoryId != -1) {
        generator.writeNumberField("inventoryId", player.inventoryId);
      }
    });
    PROPERTIES.put("location", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.location != null) {
        if (player.location == null) {
          generator.writeNullField("location");
        } else {
          generator.writeObjectFieldStart("location");
          generator.writeNumberField("x", player.location.getX());
          generator.writeNumberField("y", player.location.getY());
          generator.writeNumberField("z", player.location.getZ());
          if (context.getLatLonCalculator() != null) {
            generator.writeNumberField("lat", context.getLatLonCalculator().calculateLat(player.location.getY()));
            generator.writeNumberField("lon", context.getLatLonCalculator().calculateLon(player.location.getX()));
          }
          generator.writeEndObject();
        }
      }
    });
    PROPERTIES.put("characterLevel", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.characterLevel != 0) {
        generator.writeNumberField("characterLevel", player.characterLevel);
      }
    });
    PROPERTIES.put("experiencePoints", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.experiencePoints != 0.0f) {
        generator.writeNumberField("experiencePoints", player.experiencePoints);
      }
    });
    PROPERTIES.put("lastHypothermalCharacterInsulationValue", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.lastHypothermalCharacterInsulationValue != 0.0f) {
        generator.writeNumberField("lastHypothermalCharacterInsulationValue", player.lastHypothermalCharacterInsulationValue);
      }
    });
    PROPERTIES.put("lastHyperthermalCharacterInsulationValue", (player, generator, context, writeEmpty) -> {
      if (writeEmpty || player.lastHyperthermalCharacterInsulationValue != 0.0f) {
        generator.writeNumberField("lastHyperthermalCharacterInsulationValue", player.lastHyperthermalCharacterInsulationValue);
      }
    });
  }

  public static final List<WriterFunction<Player>> PROPERTIES_LIST = new ArrayList<>(PROPERTIES.values());

  public static final List<WriterFunction<Player>> PROPERTIES_LIST_PRIVACY =
      PROPERTIES.keySet().stream().filter(key -> !key.equals("steamId") && !key.equals("savedNetworkAddress")).map(PROPERTIES::get).collect(Collectors.toList());

  public void writeAllProperties(JsonGenerator generator, DataContext context, boolean writeEmpty, boolean noPrivacy) throws IOException {
    if (noPrivacy) {
      for (WriterFunction<Player> writer: PROPERTIES_LIST) {
        writer.accept(this, generator, context, writeEmpty);
      }
    } else {
      for (WriterFunction<Player> writer: PROPERTIES_LIST_PRIVACY) {
        writer.accept(this, generator, context, writeEmpty);
      }
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
