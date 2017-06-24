package qowyn.ark.tools.data;

import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.tools.CreatureData;
import qowyn.ark.tools.DataManager;
import qowyn.ark.types.ArkByteValue;
import qowyn.ark.types.LocationData;
import qowyn.ark.types.ObjectReference;

public class Creature {

  public static final int COLOR_SLOT_COUNT = 6;

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

  public float tameIneffectivenessModifier;

  public int tamedFollowTarget;

  public int tamingTeamID;

  public String tamedOnServerName;

  public String uploadedFromServerName;

  public int tamedAggressionLevel;

  public float matingProgress;

  public double lastEnterStasisTime;

  public Creature(GameObject creature, GameObjectContainer saveFile) {
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

    wildRandomScale = creature.findPropertyValue("WildRandomScale", Float.class).orElse(1.0f);

    isWakingTame = creature.findPropertyValue("bIsWakingTame", Boolean.class).orElse(false);

    isSleeping = creature.findPropertyValue("bIsSleeping", Boolean.class).orElse(false);

    requiredTameAffinity = creature.findPropertyValue("RequiredTameAffinity", Float.class).orElse(0.0f);

    currentTameAffinity = creature.findPropertyValue("CurrentTameAffinity", Float.class).orElse(0.0f);

    tameIneffectivenessModifier = creature.findPropertyValue("TameIneffectivenessModifier", Float.class).orElse(0.0f);

    tamedFollowTarget = creature.findPropertyValue("TamedFollowTarget", ObjectReference.class).map(ObjectReference::getObjectId).orElse(-1);

    tamingTeamID = creature.findPropertyValue("TamingTeamID", Integer.class).orElse(0);

    tamedOnServerName = creature.findPropertyValue("TamedOnServerName", String.class).orElse("");

    uploadedFromServerName = creature.findPropertyValue("UploadedFromServerName", String.class).orElse("");

    tamedAggressionLevel = creature.findPropertyValue("TamedAggressionLevel", Integer.class).orElse(0);

    matingProgress = creature.findPropertyValue("MatingProgress", Float.class).orElse(0.0f);

    lastEnterStasisTime = creature.findPropertyValue("LastEnterStasisTime", Double.class).orElse(0.0);

    GameObject status = creature.findPropertyValue("MyCharacterStatusComponent", ObjectReference.class).map(saveFile::getObject).orElse(null);

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
    }
  }

}
