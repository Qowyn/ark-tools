
package qowyn.ark.tools;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class App {

  private static final Map<String, Consumer<String[]>> COMMAND_MAP = new HashMap<>();

  static {
    COMMAND_MAP.put("animals", AnimalListCommands::animals);
    COMMAND_MAP.put("tamed", AnimalListCommands::tamed);
    COMMAND_MAP.put("wild", AnimalListCommands::wild);

    COMMAND_MAP.put("b2j", ConversionCommands::binary2json);
    COMMAND_MAP.put("j2b", ConversionCommands::json2binary);

    COMMAND_MAP.put("classes", DebugCommands::classes);

    COMMAND_MAP.put("feed", EditCommands::feed);
    COMMAND_MAP.put("export", EditCommands::exportThing);
    COMMAND_MAP.put("import", EditCommands::importThing);
  }

  public static void main(String[] args) throws Exception {
    if (args.length < 1 || !COMMAND_MAP.containsKey(args[0])) {
      System.out.println("Commands:");
      System.out.print("\t");
      System.out.println(String.join(", ", COMMAND_MAP.keySet()));
      return;
    }

    COMMAND_MAP.get(args[0]).accept(Arrays.copyOfRange(args, 1, args.length));
  }

}
