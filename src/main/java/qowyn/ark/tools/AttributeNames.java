package qowyn.ark.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class AttributeNames {

  private static final Map<Integer, String> ATTRIBUTE_NAME_MAP = new HashMap<>();

  static {
    ATTRIBUTE_NAME_MAP.put(0, "health");
    ATTRIBUTE_NAME_MAP.put(1, "stamina");
    ATTRIBUTE_NAME_MAP.put(2, "torpor");
    ATTRIBUTE_NAME_MAP.put(3, "oxygen");
    ATTRIBUTE_NAME_MAP.put(4, "food");
    ATTRIBUTE_NAME_MAP.put(5, "water");
    ATTRIBUTE_NAME_MAP.put(6, "temperature");
    ATTRIBUTE_NAME_MAP.put(7, "weight");
    ATTRIBUTE_NAME_MAP.put(8, "melee");
    ATTRIBUTE_NAME_MAP.put(9, "speed");
    ATTRIBUTE_NAME_MAP.put(10, "fortitude");
    ATTRIBUTE_NAME_MAP.put(11, "crafting");
  }

  public static String get(int index) {
    return ATTRIBUTE_NAME_MAP.get(index);
  }

  public static void forEach(BiConsumer<Integer, String> action) {
    ATTRIBUTE_NAME_MAP.forEach(action);
  }

}
