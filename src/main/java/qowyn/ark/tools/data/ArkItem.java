package qowyn.ark.tools.data;

import java.util.Optional;

import javax.json.JsonObject;

import qowyn.ark.GameObject;
import qowyn.ark.PropertyContainer;
import qowyn.ark.types.ArkByteValue;
import qowyn.ark.types.ArkName;

public class ArkItem {
  
  public boolean canSlot;

  public boolean isEngram;

  public boolean isBlueprint;
  
  public boolean canRemove;

  public boolean isHidden;

  public ArkName className;

  public int quantity;

  public String customName;

  public String customDescription;

  public float durability;

  public byte quality;

  public final short[] itemStatValues = new short[ItemStatDefinitions.size()];

  public ArkItem() {
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
    this.className = item.getClassName();

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
  public ArkItem(PropertyContainer item, ArkName className) {
    this.className = className;

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
    this.className = new ArkName(object.getString("className"));

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
      itemStatValues[i] = (short) object.getInt("itemStatsValue_"+i, 0);
    }
  }

}
