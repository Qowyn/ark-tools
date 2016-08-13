package qowyn.ark.tools;

import java.io.IOException;
import java.util.List;

import javax.json.JsonObject;

import qowyn.ark.ArkProfile;
import qowyn.ark.ArkSavegame;
import qowyn.ark.ArkTribe;

public class ConversionCommands {

  public static void mapToJson(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      System.out.println("Converts 'ARK' from binary ark format to a (huge) JSON object and writes it to 'JSON'.");
      System.out.println("Usage: ark-tools m2j ARK JSON [OPTIONS]");
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

  public static void jsonToMap(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      System.out.println("Converts 'JSON' from JSON format to the binary ark format and writes it to 'ARK'.");
      System.out.println("Usage: ark-tools j2m JSON ARK [OPTIONS]");
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
      System.out.println("Converts 'PROFILE' from binary ark format to a JSON representation and writes it to 'JSON'.");
      System.out.println("Usage: ark-tools p2j PROFILE JSON [OPTIONS]");
      oh.printHelp();
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
      System.out.println("Converts 'JSON' from JSON format to the binary ark format and writes it to 'PROFILE'.");
      System.out.println("Usage: ark-tools j2p JSON PROFILE [OPTIONS]");
      oh.printHelp();
      System.exit(1);
      return;
    }

    try {
      String jsonPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonObject object = CommonFunctions.readJson(jsonPath);
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
      System.out.println("Converts 'TRIBE' from binary ark format to a JSON representation and writes it to 'JSON'.");
      System.out.println("Usage: ark-tools t2j TRIBE JSON [OPTIONS]");
      oh.printHelp();
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
      System.out.println("Converts 'JSON' from JSON format to the binary ark format and writes it to 'TRIBE'.");
      System.out.println("Usage: ark-tools j2t JSON TRIBE [OPTIONS]");
      oh.printHelp();
      System.exit(1);
      return;
    }

    try {
      String jsonPath = params.get(0);
      String outPath = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonObject object = CommonFunctions.readJson(jsonPath);
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

}
