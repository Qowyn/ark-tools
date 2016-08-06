package qowyn.ark.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.ReadingOptions;
import qowyn.ark.WritingOptions;
import qowyn.ark.properties.PropertyFloat;

public class EditCommands {

  public static void feed(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      System.out.println("Usage: ark-tools feed <save> <newsave> [options]");
      oh.printHelp();
      System.exit(1);
      return;
    }

    Path fileToRead = Paths.get(params.get(0)).toAbsolutePath();
    Path fileToWrite = Paths.get(params.get(1)).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.err.println("save and newsave need to be different paths");
      System.exit(2);
      return;
    }

    try {
      ReadingOptions readingOptions = ReadingOptions.create()
          .withMemoryMapping(oh.useMmap())
          .withParallelReading(oh.useParallel());

      WritingOptions writingOptions = WritingOptions.create()
          .withMemoryMapping(oh.useMmap())
          .withParallelWriting(oh.useParallel());

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      ArkSavegame savegame = new ArkSavegame(fileToRead.toString(), readingOptions);

      stopwatch.stop("Reading");

      savegame.getObjects().parallelStream().filter(CommonFunctions::onlyTamed).forEach(object -> {
        PropertyFloat currentFood = object.getTypedProperty("CurrentStatusValues", PropertyFloat.class, 4);
        if (currentFood != null) {
          currentFood.setValue(1000000.0f);
        }
      });

      stopwatch.stop("Setting values");

      savegame.writeBinary(fileToWrite.toString(), writingOptions);

      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void exportThing(OptionHandler oh) {
    OptionSpec<String> dinoSpec = oh.accepts("dino", "Export dino by Name").withRequiredArg();
    OptionSpec<Integer> objectSpec = oh.accepts("object", "Export object by Id").withRequiredArg().ofType(Integer.class);

    OptionSet options = oh.reparse();

    List<String> params = oh.getParams(options);

    if (params.size() != 2 || oh.wantsHelp() || !(options.has(dinoSpec) || options.has(objectSpec))) {
      System.out.println("Usage: ark-tools export <save> --dino <name> <outfile> [options]");
      System.out.println("       ark-tools export <save> --object <id> <outfile> [options]");
      oh.printHelp();
      System.exit(1);
      return;
    }

    Path fileToRead = Paths.get(params.get(0)).toAbsolutePath();
    Path fileToWrite = Paths.get(params.get(1)).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.err.println("save and outfile need to be different paths");
      System.exit(2);
      return;
    }

    try {
      ReadingOptions readingOptions = ReadingOptions.create()
          .withMemoryMapping(oh.useMmap())
          .withParallelReading(oh.useParallel());

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      ArkSavegame savegame = new ArkSavegame(fileToRead.toString(), readingOptions);

      stopwatch.stop("Loading");

      ObjectCollector collector = null;
      if (options.has(dinoSpec)) {
        String name = dinoSpec.value(options);

        for (GameObject go : savegame.getObjects()) {
          String objectName = go.getPropertyValue("TamedName", String.class);
          if (name.equals(objectName)) {
            collector = new ObjectCollector(savegame, go);
            break;
          }
        }

        if (collector == null) {
          System.err.println("Could not find a dino named " + name);
          System.exit(2);
          return;
        }

      } else if (options.has(objectSpec)) {
        int id = objectSpec.value(options);

        if (id < 0 || id >= savegame.getObjects().size()) {
          System.err.println("Invalid object id " + id);
          System.exit(2);
          return;
        }

        collector = new ObjectCollector(savegame, savegame.getObjects().get(id));
      }

      stopwatch.stop("Collecting");

      ArkSavegame export = new ArkSavegame();

      export.setSaveVersion(savegame.getSaveVersion());
      export.setObjects(collector.remap(0));

      stopwatch.stop("Remapping");

      CommonFunctions.writeJson(fileToWrite.toString(), export.toJson());

      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void importThing(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 3 || oh.wantsHelp()) {
      System.out.println("Usage: ark-tools import <save> <jsonFile> <outfile> [options]");
      oh.printHelp();
      System.exit(1);
      return;
    }

    Path fileToRead = Paths.get(params.get(0)).toAbsolutePath();
    Path fileToWrite = Paths.get(params.get(2)).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.out.println("save and outfile need to be different paths");
      System.exit(2);
      return;
    }

    try {
      ReadingOptions readingOptions = ReadingOptions.create()
          .withMemoryMapping(oh.useMmap())
          .withParallelReading(oh.useParallel());

      WritingOptions writingOptions = WritingOptions.create()
          .withMemoryMapping(oh.useMmap())
          .withParallelWriting(oh.useParallel());

      ArkSavegame savegame = new ArkSavegame(fileToRead.toString(), readingOptions);
      ArkSavegame jsonFile = new ArkSavegame(CommonFunctions.readJson(params.get(1)), readingOptions);

      ObjectCollector collector = new ObjectCollector(jsonFile, jsonFile.getObjects().get(0));

      savegame.getObjects().addAll(collector.remap(savegame.getObjects().size()));

      savegame.writeBinary(fileToWrite.toString(), writingOptions);

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
