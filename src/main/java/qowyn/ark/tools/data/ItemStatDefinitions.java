package qowyn.ark.tools.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ItemStatDefinitions {
  
  private static final Map<Integer, String> ITEM_STAT_DEFINITIONS_MAP = new HashMap<>();

  static {
    ITEM_STAT_DEFINITIONS_MAP.put(0, "Effectiveness");
    ITEM_STAT_DEFINITIONS_MAP.put(1, "Armor");
    ITEM_STAT_DEFINITIONS_MAP.put(2, "Max Durability");
    ITEM_STAT_DEFINITIONS_MAP.put(3, "Weapon Damage");
    ITEM_STAT_DEFINITIONS_MAP.put(4, "Weapon Clip Ammo");
    ITEM_STAT_DEFINITIONS_MAP.put(5, "Hypothermic Insulation");
    ITEM_STAT_DEFINITIONS_MAP.put(6, "Weight");
    ITEM_STAT_DEFINITIONS_MAP.put(7, "Hyperthermic Insulation");
  }

  public static String get(int index) {
    return ITEM_STAT_DEFINITIONS_MAP.get(index);
  }

  public static void forEach(BiConsumer<Integer, String> action) {
    ITEM_STAT_DEFINITIONS_MAP.forEach(action);
  }
  
  public static int size() {
    return ITEM_STAT_DEFINITIONS_MAP.size();
  }

}
