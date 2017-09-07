package qowyn.ark.tools;

import java.io.IOException;
import java.util.List;

import javax.json.JsonObject;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkCloudInventory;
import qowyn.ark.ArkLocalProfile;
import qowyn.ark.ArkProfile;
import qowyn.ark.ArkSavFile;
import qowyn.ark.ArkSavegame;
import qowyn.ark.ArkTribe;

public class ConvertingCommands {

  public static void mapToJson(OptionHandler oh) {
    OptionSpec<Void> allowBrokenFileSpec = oh.accepts("allow-broken-file", "Tries to read as much of broken/truncated files as possible");

    OptionSet options = oh.reparse();
    List<String> params = oh.getParams(options);
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      if (!oh.isQuiet()) {
        System.out.println("This may take some time...");
      }

      String savePath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      ArkSavegame saveFile = new ArkSavegame();
      try {
        saveFile.readBinary(savePath, oh.readingOptions());
      } catch (Exception e) {
        if (!options.has(allowBrokenFileSpec)) {
          throw e;
        }
      }
      stopwatch.stop("Reading");
      if (saveFile != null) {
        CommonFunctions.writeJson(outPath, g -> saveFile.writeJson(g, oh.writingOptions()), oh);
      }
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void jsonToMap(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      if (!oh.isQuiet()) {
        System.out.println("This may take some time...");
      }

      String jsonPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonObject object = (JsonObject) CommonFunctions.readJson(jsonPath);
      stopwatch.stop("Parsing");
      ArkSavegame saveFile = new ArkSavegame(object, oh.readingOptions());
      stopwatch.stop("Loading");
      saveFile.writeBinary(outPath, oh.writingOptions());
      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void profileToJson(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String savePath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      ArkProfile profile = new ArkProfile(savePath);
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, profile.toJson(), oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void jsonToProfile(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String jsonPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonObject object = (JsonObject) CommonFunctions.readJson(jsonPath);
      stopwatch.stop("Parsing");
      ArkProfile profile = new ArkProfile(object);
      stopwatch.stop("Loading");
      profile.writeBinary(outPath, oh.writingOptions());
      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void tribeToJson(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String savePath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      ArkTribe tribe = new ArkTribe(savePath);
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, tribe.toJson(), oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void jsonToTribe(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String jsonPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonObject object = (JsonObject) CommonFunctions.readJson(jsonPath);
      stopwatch.stop("Parsing");
      ArkTribe tribe = new ArkTribe(object);
      stopwatch.stop("Loading");
      tribe.writeBinary(outPath, oh.writingOptions());
      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void cloudToJson(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String cloudPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      ArkCloudInventory cloudInventory = new ArkCloudInventory(cloudPath);
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, cloudInventory.toJson(), oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void jsonToCloud(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String jsonPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonObject object = (JsonObject) CommonFunctions.readJson(jsonPath);
      stopwatch.stop("Parsing");
      ArkCloudInventory cloudInventory = new ArkCloudInventory(object);
      stopwatch.stop("Loading");
      cloudInventory.writeBinary(outPath, oh.writingOptions());
      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void localProfileToJson(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String inPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      ArkLocalProfile localProfile = new ArkLocalProfile(inPath);
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, localProfile.toJson(), oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void jsonToLocalProfile(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String jsonPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonObject object = (JsonObject) CommonFunctions.readJson(jsonPath);
      stopwatch.stop("Parsing");
      ArkLocalProfile localProfile = new ArkLocalProfile(object);
      stopwatch.stop("Loading");
      localProfile.writeBinary(outPath, oh.writingOptions());
      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  public static void savToJson(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String inPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      ArkSavFile savFile = new ArkSavFile(inPath);
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, savFile.toJson(), oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void jsonToSav(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      String jsonPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonObject object = (JsonObject) CommonFunctions.readJson(jsonPath);
      stopwatch.stop("Parsing");
      ArkSavFile savFile = new ArkSavFile(object);
      stopwatch.stop("Loading");
      savFile.writeBinary(outPath, oh.writingOptions());
      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
