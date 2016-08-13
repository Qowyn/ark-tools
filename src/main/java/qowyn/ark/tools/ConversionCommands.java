package qowyn.ark.tools;

import java.io.IOException;
import java.util.List;

import javax.json.JsonObject;

import qowyn.ark.ArkProfile;
import qowyn.ark.ArkSavegame;
import qowyn.ark.ArkTribe;

public class ConversionCommands {

  public static void binary2json(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      System.out.println("Converts 'save' from binary ark format to a much bigger JSON representation.");
      System.out.println("Usage: ark-tools b2j <save> <outfile> [options]");
      oh.printHelp();
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
      ArkSavegame saveFile = new ArkSavegame(savePath, oh.readingOptions());
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, g -> saveFile.writeJson(g, oh.writingOptions()), oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void json2binary(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      System.out.println("Converts 'json' from JSON format to the binary ark format.");
      System.out.println("Usage: ark-tools j2b <json> <outfile> [options]");
      oh.printHelp();
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
      JsonObject object = CommonFunctions.readJson(jsonPath);
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
      System.out.println("Converts 'profile' from binary ark format to a much bigger JSON representation.");
      System.out.println("Usage: ark-tools p2j <profile> <outfile> [options]");
      oh.printHelp();
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
      ArkProfile profile = new ArkProfile(savePath);
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, profile.toJson(), oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void tribeToJson(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      System.out.println("Converts 'tribe' from binary ark format to a much bigger JSON representation.");
      System.out.println("Usage: ark-tools t2j <tribe> <outfile> [options]");
      oh.printHelp();
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
      ArkTribe tribe = new ArkTribe(savePath);
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, tribe.toJson(), oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
