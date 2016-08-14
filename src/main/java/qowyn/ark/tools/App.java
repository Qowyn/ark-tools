
package qowyn.ark.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import joptsimple.OptionException;

public class App {

  private static final Map<String, Command> COMMAND_NAME_MAP = new HashMap<>();

  private static final Map<String, List<Command>> COMMAND_CATEGORY_MAP = new HashMap<>();

  private static void addCommand(String category, String description, String options, Consumer<OptionHandler> action, String... names) {
    String namesString = String.join(", ", names);
    Command command = new Command(namesString, category, description, options, action);

    for (String name : names) {
      COMMAND_NAME_MAP.put(name, command);
    }

    COMMAND_CATEGORY_MAP.computeIfAbsent(category, key -> new ArrayList<>()).add(command);
  }

  static {
    addCommand("Creatures", "Writes lists of all creatures in SAVE to the specified DIRECTORY.", "SAVE DIRECTORY [OPTIONS]", CreatureListCommands::creatures, "creatures");
    addCommand("Creatures", "Writes lists of tamed creatures in SAVE to the specified DIRECTORY.", "SAVE DIRECTORY [OPTIONS]", CreatureListCommands::tamed, "tamed");
    addCommand("Creatures", "Writes lists of wild creatures in SAVE to the specified DIRECTORY.", "SAVE DIRECTORY [OPTIONS]", CreatureListCommands::wild, "wild");

    addCommand("Converting", "Converts from .ark to .json", "ARK JSON [OPTIONS]", ConvertingCommands::mapToJson, "m2j", "mapToJson");
    addCommand("Converting", "Converts from .arkprofile to .json", "PROFILE JSON [OPTIONS]", ConvertingCommands::profileToJson, "p2j", "profileToJson");
    addCommand("Converting", "Converts from .arktribe to .json", "TRIBE JSON [OPTIONS]", ConvertingCommands::tribeToJson, "t2j", "tribeToJson");

    addCommand("Converting", "Converts from .json to .ark", "ARK JSON [OPTIONS]", ConvertingCommands::jsonToMap, "j2m", "jsonToMap");
    addCommand("Converting", "Converts from .json to .arkprofile", "PROFILE JSON [OPTIONS]", ConvertingCommands::jsonToProfile, "j2p", "jsonToProfile");
    addCommand("Converting", "Converts from .json to .arktribe", "TRIBE JSON [OPTIONS]", ConvertingCommands::jsonToTribe, "j2t", "jsonToTribe");

    addCommand("Debug", "Dumps a list of all classes with count of objects to stdout or OUT_FILE.", "SAVE [OUT_FILE] [OPTIONS]", DebugCommands::classes, "classes");
    addCommand("Debug", "Dumps all objects of given CLASS_NAME to stdout or OUT_FILE.", "SAVE CLASS_NAME [OUT_FILE] [OPTIONS]", DebugCommands::dump, "dump");
    addCommand("Debug", "Dumps className and size in bytes of all objects to stdout or OUT_FILE.", "SAVE [OUT_FILE] [OPTIONS]", DebugCommands::sizes, "sizes");

    addCommand("Editing",
        "Sets food of all tamed creatures to max and brings them into the present. "
            + "Mainly useful if you left your server running with no players online.",
        "SAVE NEW_SAVE [OPTIONS]", EditingCommands::feed, "feed");
    addCommand("Editing",
        "Export a specified object/dino and everything attached to it. "
            + "Can be used to 'revive' dinos from backups or to import bases from another save file. "
            + "Or to do whatever else you want. "
            + "Manually editing exported file might be required.",
        "SAVE JSON [OPTIONS]", EditingCommands::exportThing, "export");
    addCommand("Editing", "Imports all objects from JSON into SAVE.", "SAVE JSON NEW_SAVE [OPTIONS]", EditingCommands::importThing, "import");

    addCommand("Players", "Writes lists of all players in SAVE to the specified DIRECTORY.", "SAVE DIRECTORY [OPTIONS]", PlayerListCommands::players, "players");
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

    if (!oh.hasCommand() || !COMMAND_NAME_MAP.containsKey(oh.getCommand())) {
      System.out.println("Usage: ark-tools command [OPTIONS]");
      System.out.println();

      List<String> sortedCategories = new ArrayList<>(COMMAND_CATEGORY_MAP.keySet());
      sortedCategories.sort(String::compareTo);
      for (String category : sortedCategories) {
        System.out.println(category);

        List<Command> commands = COMMAND_CATEGORY_MAP.get(category);
        int maxLength = commands.stream().mapToInt(c -> c.getNames().length()).max().getAsInt();

        for (Command command : commands) {
          System.out.println("\t" + String.format("%-" + maxLength + "s", command.getNames()) + " - " + command.getDescription());
        }
        System.out.println();
      }
      oh.printHelp();
      System.exit(1);
    }

    try {
      Command command = COMMAND_NAME_MAP.get(oh.getCommand());
      oh.setCommandObject(command);
      command.getAction().accept(oh);
    } catch (OptionException oe) {
      System.err.println(oe.getMessage());
      System.exit(2);
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(2);
    }
  }

}
