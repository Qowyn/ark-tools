package qowyn.ark.tools.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class PlayerBoneModifierNames {

  private static final Map<Integer, String> BONE_NAME_MAP = new HashMap<>();

  static {
    BONE_NAME_MAP.put(0, "head");
    BONE_NAME_MAP.put(1, "neck");
    BONE_NAME_MAP.put(2, "necklength");
    BONE_NAME_MAP.put(3, "chest");
    BONE_NAME_MAP.put(4, "shoulders");
    BONE_NAME_MAP.put(5, "armlength");
    BONE_NAME_MAP.put(6, "upperarm");
    BONE_NAME_MAP.put(7, "lowerarm");
    BONE_NAME_MAP.put(8, "hand");
    BONE_NAME_MAP.put(9, "leglength");
    BONE_NAME_MAP.put(10, "upperleg");
    BONE_NAME_MAP.put(11, "lowerleg");
    BONE_NAME_MAP.put(12, "foot");
    BONE_NAME_MAP.put(13, "hip");
    BONE_NAME_MAP.put(14, "torso");
    BONE_NAME_MAP.put(15, "upperfacesize");
    BONE_NAME_MAP.put(16, "lowerfacesize");
    BONE_NAME_MAP.put(17, "torsodepth");
    BONE_NAME_MAP.put(18, "headheight");
    BONE_NAME_MAP.put(19, "headwidth");
    BONE_NAME_MAP.put(20, "headdepth");
    BONE_NAME_MAP.put(21, "torsoheight");
  }

  public static String get(int index) {
    return BONE_NAME_MAP.get(index);
  }

  public static void forEach(BiConsumer<Integer, String> action) {
    BONE_NAME_MAP.forEach(action);
  }
  
  public static int size() {
    return BONE_NAME_MAP.size();
  }

}
