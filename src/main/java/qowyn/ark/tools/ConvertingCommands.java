package qowyn.ark.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.JsonNode;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkCloudInventory;
import qowyn.ark.ArkLocalProfile;
import qowyn.ark.ArkProfile;
import qowyn.ark.ArkSavFile;
import qowyn.ark.ArkSavegame;
import qowyn.ark.ArkTribe;
import qowyn.ark.ConversionSupport;

public class ConvertingCommands {

  public static void mapToJson(OptionHandler oh) {
    toJson(oh, ArkSavegame::new);
  }

  public static void jsonToMap(OptionHandler oh) {
    fromJson(oh, ArkSavegame::new);
  }

  public static void profileToJson(OptionHandler oh) {
    toJson(oh, ArkProfile::new);
  }

  public static void jsonToProfile(OptionHandler oh) {
    fromJson(oh, ArkProfile::new);
  }

  public static void tribeToJson(OptionHandler oh) {
    toJson(oh, ArkTribe::new);
  }

  public static void jsonToTribe(OptionHandler oh) {
    fromJson(oh, ArkTribe::new);
  }

  public static void cloudToJson(OptionHandler oh) {
    toJson(oh, ArkCloudInventory::new);
  }

  public static void jsonToCloud(OptionHandler oh) {
    fromJson(oh, ArkCloudInventory::new);
  }

  public static void localProfileToJson(OptionHandler oh) {
    toJson(oh, ArkLocalProfile::new);
  }

  public static void jsonToLocalProfile(OptionHandler oh) {
    fromJson(oh, ArkLocalProfile::new);
  }
  
  public static void savToJson(OptionHandler oh) {
    toJson(oh, ArkSavFile::new);
  }

  public static void jsonToSav(OptionHandler oh) {
    fromJson(oh, ArkSavFile::new);
  }

  public static void toJson(OptionHandler oh, Supplier<ConversionSupport> supplier) {
    OptionSpec<Void> allowBrokenFileSpec = oh.accepts("allow-broken-file", "Tries to read as much of broken/truncated files as possible");

    OptionSet options = oh.reparse();
    List<String> params = oh.getParams(options);
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      Path inPath = Paths.get(params.get(0));
      Path outPath = Paths.get(params.get(1));

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      ConversionSupport objectToConvert = supplier.get();
      try {
        objectToConvert.readBinary(inPath, oh.readingOptions());
      } catch (Exception e) {
        if (!options.has(allowBrokenFileSpec)) {
          throw e;
        }
      }
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, g -> objectToConvert.writeJson(g, oh.writingOptions()), oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void fromJson(OptionHandler oh, Supplier<ConversionSupport> supplier) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      Path inPath = Paths.get(params.get(0));
      Path outPath = Paths.get(params.get(1));

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonNode node = CommonFunctions.readJson(inPath);
      stopwatch.stop("Parsing");
      ConversionSupport objectToConvert = supplier.get();
      objectToConvert.readJson(node, oh.readingOptions());
      stopwatch.stop("Loading");
      objectToConvert.writeBinary(outPath, oh.writingOptions());
      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
