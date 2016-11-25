
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

  private static String[] list(String... list) {
    return list;
  }

  private static void addCommand(String[] names, String category, String[] options, String description, Consumer<OptionHandler> action) {
    String namesString = String.join(", ", names);
    String optionSummary = String.join(" ", options);
    Command command = new Command(namesString, category, description, optionSummary, options, action);

    for (String name : names) {
      COMMAND_NAME_MAP.put(name, command);
    }

    COMMAND_CATEGORY_MAP.computeIfAbsent(category, key -> new ArrayList<>()).add(command);
  }

  static {
    addCommand(list("creatures"), "Creatures", list("SAVE", "DIRECTORY"), "Writes lists of all creatures in SAVE to the specified DIRECTORY.", CreatureListCommands::creatures);
    addCommand(list("tamed"), "Creatures", list("SAVE", "DIRECTORY"), "Writes lists of tamed creatures in SAVE to the specified DIRECTORY.", CreatureListCommands::tamed);
    addCommand(list("wild"), "Creatures", list("SAVE", "DIRECTORY"), "Writes lists of wild creatures in SAVE to the specified DIRECTORY.", CreatureListCommands::wild);

    addCommand(list("m2j", "mapToJson"), "Converting", list("ARK", "JSON"), "Converts from .ark to .json", ConvertingCommands::mapToJson);
    addCommand(list("p2j", "profileToJson"), "Converting", list("PROFILE", "JSON"), "Converts from .arkprofile to .json", ConvertingCommands::profileToJson);
    addCommand(list("t2j", "tribeToJson"), "Converting", list("TRIBE", "JSON"), "Converts from .arktribe to .json", ConvertingCommands::tribeToJson);
    addCommand(list("c2j", "cloudToJson"), "Converting", list("CLOUD", "JSON"), "Converts cloud data to .json", ConvertingCommands::cloudToJson);
    addCommand(list("l2j", "localProfileToJson"), "Converting", list("LOCALPROFILE", "JSON"), "Converts local profile data to .json", ConvertingCommands::localProfileToJson);
    addCommand(list("s2j", "savToJson"), "Converting", list("SAV", "JSON"), "Converts .sav to .json", ConvertingCommands::savToJson);

    addCommand(list("j2m", "jsonToMap"), "Converting", list("JSON", "ARK"), "Converts from .json to .ark", ConvertingCommands::jsonToMap);
    addCommand(list("j2p", "jsonToProfile"), "Converting", list("JSON", "PROFILE"), "Converts from .json to .arkprofile", ConvertingCommands::jsonToProfile);
    addCommand(list("j2t", "jsonToTribe"), "Converting", list("JSON", "TRIBE"), "Converts from .json to .arktribe", ConvertingCommands::jsonToTribe);
    addCommand(list("j2c", "jsonToCloud"), "Converting", list("JSON", "CLOUD"), "Converts from .json to cloud data", ConvertingCommands::jsonToCloud);
    addCommand(list("j2l", "jsonToLocalProfile"), "Converting", list("JSON", "LOCALPROFILE"), "Converts from .json to local profile data", ConvertingCommands::jsonToLocalProfile);
    addCommand(list("j2s", "jsonToSav"), "Converting", list("JSON", "SAV"), "Converts from .json to .sav", ConvertingCommands::jsonToSav);

    addCommand(list("classes"), "Debug", list("SAVE", "[OUT_FILE]"), "Dumps a list of all classes with count of objects to stdout or OUT_FILE.", DebugCommands::classes);
    addCommand(list("dump"), "Debug", list("SAVE", "CLASS_NAME", "[OUT_FILE]"), "Dumps all objects of given CLASS_NAME to stdout or OUT_FILE.", DebugCommands::dump);
    addCommand(list("sizes"), "Debug", list("SAVE", "[OUT_FILE]"), "Dumps className and size in bytes of all objects to stdout or OUT_FILE.", DebugCommands::sizes);

    addCommand(list("feed"), "Editing", list("SAVE", "NEW_SAVE"),
        "Sets food of all tamed creatures to max and brings them into the present. "
            + "Mainly useful if you left your server running with no players online.",
        EditingCommands::feed);
    addCommand(list("export"), "Editing", list("SAVE", "JSON"),
        "Export a specified object/dino and everything attached to it. "
            + "Can be used to 'revive' dinos from backups or to import bases from another save file. "
            + "Or to do whatever else you want. "
            + "Manually editing exported file might be required.",
        EditingCommands::exportThing);
    addCommand(list("import"), "Editing", list("SAVE", "JSON", "NEW_SAVE"), "Imports all objects from JSON into SAVE.", EditingCommands::importThing);
    addCommand(list("modify"), "Editing", list("INPUT", "MODIFICATION", "OUTPUT"), "Applies the actions defined in MODIFICATION to the specified INPUT file.", EditingCommands::modify);

    addCommand(list("players"), "Players", list("SAVE", "DIRECTORY"), "Writes lists of all players in SAVE to the specified DIRECTORY.", PlayerListCommands::players);
    addCommand(list("tribes"), "Players", list("SAVE", "DIRECTORY"), "Writes lists of all tribes in SAVE to the specified DIRECTORY.", PlayerListCommands::tribes);
    addCommand(list("cluster"), "Players", list("CLUSTER_DIRECTORY", "OUTPUT_DIRECTORY"), "Writes lists of all things which players have uploaded into the cluster.", PlayerListCommands::cluster);

    addCommand(list("latlon"), "Settings", list(), "Exports internal LatLonCalculator data to latLonCalculator.json in the current working directory", SettingsCommands::latlon);
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
      if (oh.hasCommand()) {
        System.err.println("Unknown Command: " + oh.getCommand());
      }
      System.err.println("Usage: ark-tools command [OPTIONS]");
      System.err.println();

      List<String> sortedCategories = new ArrayList<>(COMMAND_CATEGORY_MAP.keySet());
      sortedCategories.sort(String::compareTo);
      for (String category : sortedCategories) {
        System.err.println(category);

        List<Command> commands = COMMAND_CATEGORY_MAP.get(category);
        int maxLength = commands.stream().mapToInt(c -> c.getNames().length()).max().getAsInt();

        for (Command command : commands) {
          System.err.println("\t" + String.format("%-" + maxLength + "s", command.getNames()) + " - " + command.getDescription());
        }
        System.err.println();
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
