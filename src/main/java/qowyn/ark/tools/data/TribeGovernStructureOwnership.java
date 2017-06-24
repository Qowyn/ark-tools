package qowyn.ark.tools.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class TribeGovernStructureOwnership {

  private static final Map<Integer, String> VALUE_MAP = new HashMap<>();

  static {
    VALUE_MAP.put(-1, "Tribe Owned, Admin Demolish");
    VALUE_MAP.put(0, "Tribe Owned");
    VALUE_MAP.put(1, "Personally Owned, Tribe Snap, Admin Demolish");
    VALUE_MAP.put(2, "Personally Owned, Personal Snap");
  }

  public static String get(int index) {
    return VALUE_MAP.get(index);
  }

  public static void forEach(BiConsumer<Integer, String> action) {
    VALUE_MAP.forEach(action);
  }
  
  public static int size() {
    return VALUE_MAP.size();
  }

}
