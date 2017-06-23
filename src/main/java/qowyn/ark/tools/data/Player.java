package qowyn.ark.tools.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import qowyn.ark.ArkProfile;
import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.PropertyContainer;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.structs.StructLinearColor;
import qowyn.ark.structs.StructPropertyList;
import qowyn.ark.structs.StructUniqueNetIdRepl;
import qowyn.ark.tools.LatLonCalculator;
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

  public int inventoryId = -1;

  public LocationData location;

  public int characterLevel;

  public float experiencePoints;

  public float lastHypothermalCharacterInsulationValue;

  public float lastHyperthermalCharacterInsulationValue;

  public final float[] currentStatusValues = new float[AttributeNames.size()];

  public Player(Path path, GameObjectContainer playersWithStats, LatLonCalculator latLonCalculator) throws IOException {
    ArkProfile profile = new ArkProfile(path.toString());

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
      numberOfLevelUpPointsApplied[i] = characterStats.findPropertyValue("CharacterStatusComponent_NumberOfLevelUpPointsApplied", ArkByteValue.class, i).map(ArkByteValue::getByteValue).orElse((byte)0);
    }

    percentageOfHeadHairGrowth = characterStats.findPropertyValue("PercentageOfHeadHairGrowth", Float.class).orElse(0.0f);
    percentageOfFacialHairGrowth = characterStats.findPropertyValue("PercentageOfFacialHairGrowth", Float.class).orElse(0.0f);

    GameObject player = null;
    for (GameObject object : playersWithStats.getObjects()) {
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
    for (GameObject object : playersWithStats.getObjects()) {
      if (object.getNames().size() == 2 && object.getNames().get(0).toString().equals("PlayerCharacterStatus") && object.getNames().get(1) == player.getNames().get(0)) {
        playerCharacterStatus = object;
        break;
      }
    }

    inventoryId = player.findPropertyValue("MyInventoryComponent", ObjectReference.class).map(ObjectReference::getObjectId).orElse(-1);
    location = player.getLocation();

    if (playerCharacterStatus != null) {
      lastHypothermalCharacterInsulationValue = playerCharacterStatus.findPropertyValue("LastHypothermalCharacterInsulationValue", Float.class).orElse(0.0f);
      lastHyperthermalCharacterInsulationValue = playerCharacterStatus.findPropertyValue("LastHyperthermalCharacterInsulationValue", Float.class).orElse(0.0f);

      for (int index = 0; index < AttributeNames.size(); index++) {
        currentStatusValues[index] = playerCharacterStatus.findPropertyValue("CurrentStatusValues", Float.class, index).orElse(0.0f);
      }
    }
  }

}
