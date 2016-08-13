
package qowyn.ark.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import joptsimple.OptionException;

public class App {

  private static final Map<String, Consumer<OptionHandler>> COMMAND_MAP = new HashMap<>();

  static {
    COMMAND_MAP.put("animals", AnimalListCommands::animals);
    COMMAND_MAP.put("tamed", AnimalListCommands::tamed);
    COMMAND_MAP.put("wild", AnimalListCommands::wild);

    COMMAND_MAP.put("m2j", ConversionCommands::mapToJson);
    COMMAND_MAP.put("p2j", ConversionCommands::profileToJson);
    COMMAND_MAP.put("t2j", ConversionCommands::tribeToJson);
    COMMAND_MAP.put("j2m", ConversionCommands::jsonToMap);

    COMMAND_MAP.put("classes", DebugCommands::classes);
    COMMAND_MAP.put("dump", DebugCommands::dump);
    COMMAND_MAP.put("sizes", DebugCommands::sizes);

    COMMAND_MAP.put("feed", EditCommands::feed);
    COMMAND_MAP.put("export", EditCommands::exportThing);
    COMMAND_MAP.put("import", EditCommands::importThing);
  }

  public static void main(String[] args) throws Exception {
    OptionHandler oh;
    try {
      oh = new OptionHandler(args);
    } catch (OptionException oe) {
      System.err.println(oe.getMessage());
      System.exit(2); // System.exit never returns normally but javac does not care
      return;
    }

    if (!oh.hasCommand() || !COMMAND_MAP.containsKey(oh.getCommand())) {
      System.out.println("Usage: ark-tools command [options]");
      System.out.println();
      System.out.print("Commands: ");
      System.out.println(String.join(", ", COMMAND_MAP.keySet().stream().sorted().collect(Collectors.toList())));
      oh.printHelp();
      System.exit(1);
    }

    try {
      COMMAND_MAP.get(oh.getCommand()).accept(oh);
    } catch (OptionException oe) {
      System.err.println(oe.getMessage());
      System.exit(2);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(2);
    }
  }

}
