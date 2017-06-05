package qowyn.ark.tools.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class PlayerBodyColorRegions {

  private static final Map<Integer, String> REGION_NAME_MAP = new HashMap<>();

  static {
    REGION_NAME_MAP.put(0, "skin");
    REGION_NAME_MAP.put(1, "head hair");
    REGION_NAME_MAP.put(2, "eyes");
    REGION_NAME_MAP.put(3, "facial hair");
  }

  public static String get(int index) {
    return REGION_NAME_MAP.get(index);
  }

  public static void forEach(BiConsumer<Integer, String> action) {
    REGION_NAME_MAP.forEach(action);
  }
  
  public static int size() {
    return REGION_NAME_MAP.size();
  }

}
