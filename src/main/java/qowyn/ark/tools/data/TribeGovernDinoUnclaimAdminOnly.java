package qowyn.ark.tools.data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class TribeGovernDinoUnclaimAdminOnly {

  private static final Map<Integer, String> VALUE_MAP = new HashMap<>();

  static {
    VALUE_MAP.put(0, "Any Tribe Member");
    VALUE_MAP.put(1, "Only Admins");
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
