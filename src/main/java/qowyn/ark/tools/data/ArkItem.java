package qowyn.ark.tools.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

import javax.json.Json;
import javax.json.JsonObject;

import qowyn.ark.GameObject;
import qowyn.ark.PropertyContainer;
import qowyn.ark.arrays.ArkArrayLong;
import qowyn.ark.data.ExtraDataZero;
import qowyn.ark.properties.PropertyArray;
import qowyn.ark.properties.PropertyBool;
import qowyn.ark.properties.PropertyByte;
import qowyn.ark.properties.PropertyDouble;
import qowyn.ark.properties.PropertyFloat;
import qowyn.ark.properties.PropertyInt16;
import qowyn.ark.properties.PropertyInt32;
import qowyn.ark.properties.PropertyObject;
import qowyn.ark.properties.PropertyStr;
import qowyn.ark.properties.PropertyStruct;
import qowyn.ark.structs.StructPropertyList;
import qowyn.ark.structs.StructVector;
import qowyn.ark.types.ArkByteValue;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.ObjectReference;

public class ArkItem {

  private static final int COLOR_SLOT_COUNT = 6;

  public boolean canEquip;

  public boolean canSlot;

  public boolean isEngram;

  public boolean isBlueprint;

  public boolean canRemove;

  public boolean isHidden;

  public ArkName className;

  public String blueprintGeneratedClass;

  public int quantity;

  public String customName;

  public String customDescription;

  public float durability;

  public byte quality;

  public final short[] itemStatValues = new short[ItemStatDefinitions.size()];

  public ArkItem() {
    canEquip = true;
    canSlot = true;
    canRemove = true;
    quantity = 1;
    customName = "";
    customDescription = "";
  }

  /**
   * From ArkSavegame
   */
  public ArkItem(GameObject item) {
    className = item.getClassName();

    canEquip = item.findPropertyValue("bAllowEquppingItem", Boolean.class).orElse(true);
    canSlot = item.findPropertyValue("bCanSlot", Boolean.class).orElse(true);
    isEngram = item.findPropertyValue("bIsEngram", Boolean.class).orElse(false);
    isBlueprint = item.findPropertyValue("bIsBlueprint", Boolean.class).orElse(false);
    canRemove = item.findPropertyValue("bAllowRemovalFromInventory", Boolean.class).orElse(true);
    isHidden = item.findPropertyValue("bHideFromInventoryDisplay", Boolean.class).orElse(false);

    quantity = item.findPropertyValue("ItemQuantity", Number.class).map(Number::intValue).orElse(1);

    customName = item.findPropertyValue("CustomItemName", String.class).orElse("");

    customDescription = item.findPropertyValue("CustomItemDescription", String.class).orElse("");

    durability = item.findPropertyValue("SavedDurability", Float.class).orElse(0.0f);

    quality = item.findPropertyValue("ItemQualityIndex", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);

    for (int i = 0; i < ItemStatDefinitions.size(); i++) {
      itemStatValues[i] = item.findPropertyValue("ItemStatValues", Short.class, i).orElse((short) 0);
    }
  }

  /**
   * From cluster storage
   */
  public ArkItem(PropertyContainer item) {
    blueprintGeneratedClass = item.getPropertyValue("ItemArchetype", ObjectReference.class).getObjectString().toString();
    className = new ArkName(blueprintGeneratedClass.substring(blueprintGeneratedClass.lastIndexOf('.') + 1));

    canEquip = true;
    canSlot = item.findPropertyValue("bIsSlot", Boolean.class).orElse(true);
    isEngram = item.findPropertyValue("bIsEngram", Boolean.class).orElse(false);
    isBlueprint = item.findPropertyValue("bIsBlueprint", Boolean.class).orElse(false);
    canRemove = item.findPropertyValue("bAllowRemovalFromInventory", Boolean.class).orElse(true);
    isHidden = item.findPropertyValue("bHideFromInventoryDisplay", Boolean.class).orElse(false);

    quantity = item.findPropertyValue("ItemQuantity", Number.class).map(Number::intValue).orElse(1);

    customName = item.findPropertyValue("CustomItemName", String.class).orElse("");

    customDescription = item.findPropertyValue("CustomItemDescription", String.class).orElse("");

    durability = item.findPropertyValue("ItemDurability", Float.class).orElse(0.0f);

    quality = item.findPropertyValue("ItemQualityIndex", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);

    for (int i = 0; i < ItemStatDefinitions.size(); i++) {
      itemStatValues[i] = item.findPropertyValue("ItemStatValues", Short.class, i).orElse((short) 0);
    }
  }

  /**
   * From JSON / ModificationFile
   */
  public ArkItem(JsonObject object) {
    className = new ArkName(object.getString("className"));
    blueprintGeneratedClass = "BlueprintGeneratedClass " + object.getString("blueprintGeneratedClass", "");

    canEquip = object.getBoolean("canEquip", true);
    canSlot = object.getBoolean("canSlot", true);
    isEngram = object.getBoolean("isEngram", false);
    isBlueprint = object.getBoolean("isBlueprint", false);
    canRemove = object.getBoolean("canRemove", true);
    isHidden = object.getBoolean("isHidden", false);

    quantity = object.getInt("quantity", 1);

    customName = object.getString("customName", "");

    customDescription = object.getString("customDescription", "");

    // Getting a float with a default value using JSR 353? Eaaaaasy
    durability = Optional.ofNullable(object.getJsonNumber("durability")).map(n -> n.bigDecimalValue().floatValue()).orElse(0.0f);

    quality = (byte) object.getInt("quality", 0);

    for (int i = 0; i < ItemStatDefinitions.size(); i++) {
      itemStatValues[i] = (short) object.getInt("itemStatsValue_" + i, 0);
    }
  }

  public StructPropertyList toClusterData() {
    if (blueprintGeneratedClass.equals("BlueprintGeneratedClass ")) {
      System.err.println("Item " + className + " is missing blueprintGeneratedClass.");
      return null;
    }

    StructPropertyList result = new StructPropertyList(Json.createArrayBuilder().build(), null);
    StructPropertyList arkTributeItem = new StructPropertyList(Json.createArrayBuilder().build(), new ArkName("ItemNetInfo"));

    result.getProperties().add(new PropertyStruct("ArkTributeItem", "StructProperty", arkTributeItem));

    ObjectReference itemArchetype = new ObjectReference();
    itemArchetype.setObjectType(ObjectReference.TYPE_PATH);
    itemArchetype.setObjectString(new ArkName(blueprintGeneratedClass));
    arkTributeItem.getProperties().add(new PropertyObject("ItemArchetype", "ObjectProperty", itemArchetype));

    Random random = new Random();
    long randomId = random.nextLong();

    StructPropertyList struct = new StructPropertyList(Json.createArrayBuilder().build(), new ArkName("ItemNetID"));

    struct.getProperties().add(new PropertyInt32("ItemID1", "UInt32Property", (int) (randomId >> 32)));
    struct.getProperties().add(new PropertyInt32("ItemID2", "UInt32Property", (int) randomId));

    arkTributeItem.getProperties().add(new PropertyStruct("ItemId", "StructProperty", struct));

    arkTributeItem.getProperties().add(new PropertyBool("bIsBlueprint", "BoolProperty", isBlueprint));
    arkTributeItem.getProperties().add(new PropertyBool("bIsEngram", "BoolProperty", isEngram));
    arkTributeItem.getProperties().add(new PropertyBool("bIsCustomRecipe", "BoolProperty", false));
    arkTributeItem.getProperties().add(new PropertyBool("bIsFoodRecipe", "BoolProperty", false));
    arkTributeItem.getProperties().add(new PropertyBool("bIsRepairing", "BoolProperty", false));
    arkTributeItem.getProperties().add(new PropertyBool("bAllowRemovalFromInventory", "BoolProperty", canRemove));
    arkTributeItem.getProperties().add(new PropertyBool("bAllowRemovalFromSteamInventory", "BoolProperty", true));
    arkTributeItem.getProperties().add(new PropertyBool("bHideFromInventoryDisplay", "BoolProperty", isHidden));
    arkTributeItem.getProperties().add(new PropertyBool("bFromSteamInventory", "BoolProperty", false));
    arkTributeItem.getProperties().add(new PropertyBool("bIsFromAllClustersInventory", "BoolProperty", false));
    arkTributeItem.getProperties().add(new PropertyBool("bIsEquipped", "BoolProperty", false));
    arkTributeItem.getProperties().add(new PropertyBool("bIsSlot", "BoolProperty", canSlot));
    arkTributeItem.getProperties().add(new PropertyInt32("ExpirationTimeUTC", "UInt32Property", 0));
    arkTributeItem.getProperties().add(new PropertyInt32("ItemQuantity", "IntProperty", quantity));
    arkTributeItem.getProperties().add(new PropertyStr("CustomItemName", "StrProperty", customName));
    arkTributeItem.getProperties().add(new PropertyStr("CustomItemDescription", "StrProperty", customDescription));
    arkTributeItem.getProperties().add(new PropertyFloat("ItemDurability", "FloatProperty", durability));
    arkTributeItem.getProperties().add(new PropertyFloat("ItemRating", "FloatProperty", 0.0f));

    ArkByteValue qualityValue = new ArkByteValue();
    qualityValue.setByteValue(quality);
    arkTributeItem.getProperties().add(new PropertyByte("ItemQualityIndex", "ByteProperty", qualityValue));

    for (int i = 0; i < itemStatValues.length; i++) {
      arkTributeItem.getProperties().add(new PropertyInt16("ItemStatValues", "UInt16Property", i, itemStatValues[i]));
    }

    // TODO: add other values

    arkTributeItem.getProperties().add(new PropertyDouble("NextSpoilingTime", "DoubleProperty", 0.0));
    arkTributeItem.getProperties().add(new PropertyDouble("LastSpoilingTime", "DoubleProperty", 0.0));

    ObjectReference lastOwnerPlayer = new ObjectReference();
    lastOwnerPlayer.setLength(4);
    lastOwnerPlayer.setObjectId(-1);
    lastOwnerPlayer.setObjectType(ObjectReference.TYPE_ID);
    arkTributeItem.getProperties().add(new PropertyObject("LastOwnerPlayer", "ObjectProperty", lastOwnerPlayer));

    arkTributeItem.getProperties().add(new PropertyDouble("LastAutoDurabilityDecreaseTime", "DoubleProperty", 0.0));
    arkTributeItem.getProperties().add(new PropertyStruct("OriginalItemDropLocation", "StructProperty", new StructVector(Json.createObjectBuilder().build(), new ArkName("Vector"))));

    for (int i = 0; i < COLOR_SLOT_COUNT; i++) {
      ArkByteValue value = new ArkByteValue();
      value.setByteValue((byte) 0);
      arkTributeItem.getProperties().add(new PropertyByte("PreSkinItemColorID", "ByteProperty", i, value));
    }

    for (int i = 0; i < AttributeNames.size(); i++) {
      ArkByteValue value = new ArkByteValue();
      value.setByteValue((byte) 0);
      arkTributeItem.getProperties().add(new PropertyByte("EggNumberOfLevelUpPointsApplied", "ByteProperty", i, value));
    }

    arkTributeItem.getProperties().add(new PropertyFloat("EggTamedIneffectivenessModifier", "FloatProperty", 0.0f));

    for (int i = 0; i < COLOR_SLOT_COUNT; i++) {
      ArkByteValue value = new ArkByteValue();
      value.setByteValue((byte) 0);
      arkTributeItem.getProperties().add(new PropertyByte("EggColorSetIndices", "ByteProperty", i, value));
    }

    ArkByteValue itemVersion = new ArkByteValue();
    itemVersion.setByteValue((byte) 0);
    arkTributeItem.getProperties().add(new PropertyByte("ItemVersion", "ByteProperty", itemVersion));
    arkTributeItem.getProperties().add(new PropertyInt32("CustomItemID", "IntProperty", 0));
    arkTributeItem.getProperties().add(new PropertyArray("SteamUserItemID", "ArrayProperty", new ArkArrayLong(), new ArkName("UInt64Property")));

    result.getProperties().add(new PropertyFloat("Version", "FloatProperty", 2.0f));
    result.getProperties().add(new PropertyInt32("UploadTime", "IntProperty", (int) Instant.now().getEpochSecond()));

    return result;
  }

  public GameObject toGameObject(Collection<GameObject> existingObjects, int ownerInventory) {
    GameObject object = new GameObject();

    object.setClassName(className);

    if (!canEquip) {
      object.getProperties().add(new PropertyBool("bAllowEquppingItem", "BoolProperty", canEquip));
    }

    if (!canSlot) {
      object.getProperties().add(new PropertyBool("bCanSlot", "BoolProperty", canSlot));
    }

    if (isEngram) {
      object.getProperties().add(new PropertyBool("bIsEngram", "BoolProperty", isEngram));
    }

    if (isBlueprint) {
      object.getProperties().add(new PropertyBool("bIsBlueprint", "BoolProperty", isBlueprint));
    }

    if (!canRemove) {
      object.getProperties().add(new PropertyBool("bAllowRemovalFromInventory", "BoolProperty", canRemove));
    }

    if (isHidden) {
      object.getProperties().add(new PropertyBool("bHideFromInventoryDisplay", "BoolProperty", isHidden));
    }

    if (quantity != 1) {
      object.getProperties().add(new PropertyInt32("ItemQuantity", "IntProperty", quantity));
    }

    if (!customName.isEmpty()) {
      object.getProperties().add(new PropertyStr("CustomItemName", "StrProperty", customName));
    }

    if (!customDescription.isEmpty()) {
      object.getProperties().add(new PropertyStr("CustomItemDescription", "StrProperty", customDescription));
    }

    if (durability > 0) {
      object.getProperties().add(new PropertyFloat("SavedDurability", "FloatProperty", durability));
    }

    if (quality > 0) {
      ArkByteValue value = new ArkByteValue();
      value.setByteValue(quality);
      object.getProperties().add(new PropertyByte("ItemQualityIndex", "ByteProperty", value));
    }

    for (int i = 0; i < itemStatValues.length; i++) {
      if (itemStatValues[i] != 0) {
        object.getProperties().add(new PropertyInt16("ItemStatValues", "UInt16Property", i, itemStatValues[i]));
      }
    }

    Set<Long> itemIDs = new HashSet<>(); // Stored as StructPropertyList with 2 UInt32
    Set<ArkName> names = new HashSet<>();

    for (GameObject existingObject : existingObjects) {
      existingObject.getNames().forEach(names::add);

      PropertyContainer itemID = existingObject.getPropertyValue("ItemId", PropertyContainer.class);
      if (itemID != null) {
        Integer itemID1 = itemID.getPropertyValue("ItemID1", Integer.class);
        Integer itemID2 = itemID.getPropertyValue("ItemID2", Integer.class);
        if (itemID1 != null && itemID2 != null) {
          long id = (long) itemID1 << Integer.SIZE | (itemID2 & 0xFFFFFFFFL);
          itemIDs.add(id);
          continue;
        }
      }
    }

    Random random = new Random();

    Function<ArkName, ArkName> findFreeName = name -> {
      for (int i = 1; i < Integer.MAX_VALUE; i++) {
        ArkName tempName = new ArkName(name.getNameString(), i);
        if (!names.contains(tempName)) {
          return tempName;
        }
      }

      throw new Error("This is insane.");
    };

    long randomId = random.nextLong();
    while (itemIDs.contains(randomId)) {
      randomId = random.nextLong();
    }

    StructPropertyList struct = new StructPropertyList(Json.createArrayBuilder().build(), new ArkName("ItemNetID"));

    struct.getProperties().add(new PropertyInt32("ItemID1", "UInt32Property", (int) (randomId >> 32)));
    struct.getProperties().add(new PropertyInt32("ItemID2", "UInt32Property", (int) randomId));

    object.getProperties().add(new PropertyStruct("ItemId", "StructProperty", struct));

    object.setNames(new ArrayList<>());
    object.getNames().add(findFreeName.apply(className));

    object.setItem(true);

    ObjectReference ownerInventoryReference = new ObjectReference();
    ownerInventoryReference.setLength(8);
    ownerInventoryReference.setObjectId(ownerInventory);
    ownerInventoryReference.setObjectType(ObjectReference.TYPE_ID);

    object.getProperties().add(new PropertyObject("OwnerInventory", "ObjectProperty", ownerInventoryReference));
    object.setExtraData(new ExtraDataZero());

    return object;
  }

}
