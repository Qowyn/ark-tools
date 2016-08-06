package qowyn.ark.tools;

import java.io.IOException;
import java.util.List;

import javax.json.JsonObject;

import qowyn.ark.ArkSavegame;
import qowyn.ark.ReadingOptions;
import qowyn.ark.WritingOptions;

public class ConversionCommands {

  public static void binary2json(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
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

      ReadingOptions options = ReadingOptions.create()
          .withMemoryMapping(oh.useMmap())
          .withParallelReading(oh.useParallel());

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      ArkSavegame saveFile = new ArkSavegame(savePath, options);
      stopwatch.stop("Reading");
      CommonFunctions.writeJson(outPath, saveFile::writeJson);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void json2binary(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
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

      WritingOptions options = WritingOptions.create()
          .withMemoryMapping(oh.useMmap())
          .withParallelWriting(oh.useParallel());

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      JsonObject object = CommonFunctions.readJson(jsonPath);
      stopwatch.stop("Parsing");
      ArkSavegame saveFile = new ArkSavegame(object);
      stopwatch.stop("Loading");
      saveFile.writeBinary(outPath, options);
      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
