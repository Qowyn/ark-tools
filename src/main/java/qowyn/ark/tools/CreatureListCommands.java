package qowyn.ark.tools;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.ReadingOptions;
import qowyn.ark.tools.data.Creature;
import qowyn.ark.tools.data.CustomDataContext;

public class CreatureListCommands {

  private static ArkSavegame saveFile;

  private static OptionHandler optionHandler;

  private static Path outputDirectory;

  private static OptionSpec<Void> untameableSpec;

  private static OptionSpec<Void> statisticsSpec;

  private static OptionSpec<Void> withoutIndexSpec;

  private static OptionSpec<Void> cleanFolderSpec;

  private static OptionSpec<Void> writeAllFieldsSpec;

  private static OptionSpec<String> inventorySpec;

  private static OptionSet options;

  private static WriteJsonCallback writerFunction;

  public static void creatures(OptionHandler optionHandler) {
    CreatureListCommands.optionHandler = optionHandler;
    listImpl(null);
  }

  public static void tamed(OptionHandler optionHandler) {
    CreatureListCommands.optionHandler = optionHandler;
    listImpl(CommonFunctions::onlyTamed);
  }

  public static void wild(OptionHandler optionHandler) {
    CreatureListCommands.optionHandler = optionHandler;
    listImpl(CommonFunctions::onlyWild);
  }

  protected static boolean neededClasses(GameObject object) {
    return object.getClassString().contains("_Character_") || object.getClassString().startsWith("DinoCharacterStatusComponent_") || object.getClassString().equals("Raft_BP_C");
  }

  protected static boolean onlyTameable(GameObject object) {
    return (!object.hasAnyProperty("bForceDisablingTaming") || !object.getPropertyValue("bForceDisablingTaming", Boolean.class)) || object.getClassString().equals("Raft_BP_C");
  }

  protected static boolean onlyCreatures(GameObject object) {
    return object.getClassString().contains("_Character_") || object.getClassString().equals("Raft_BP_C");
  }

  protected static void listImpl(BiPredicate<GameObject, ArkSavegame> filter) {
    try {
      untameableSpec = optionHandler.accepts("include-untameable", "Include untameable high-level dinos.");
      statisticsSpec = optionHandler.accepts("statistics", "Wrap list of dinos in statistics block.");
      withoutIndexSpec = optionHandler.accepts("without-index", "Omits reading and writing classes.json");
      cleanFolderSpec = optionHandler.accepts("clean", "Deletes all .json files in the target directory.");
      writeAllFieldsSpec = optionHandler.accepts("write-all-fields", "Writes all the fields.");
      inventorySpec = optionHandler.accepts("inventory", "Include inventory of creatures.").withOptionalArg().describedAs("summary|long").defaultsTo("summary");

      options = optionHandler.reparse();

      List<String> params = optionHandler.getParams(options);
      if (params.size() != 2 || optionHandler.wantsHelp()) {
        optionHandler.printCommandHelp();
        System.exit(1);
        return;
      }

      if (!options.has(withoutIndexSpec)) {
        DataManager.loadData(optionHandler.lang());
      }

      Path savePath = Paths.get(params.get(0));
      outputDirectory = Paths.get(params.get(1));

      ReadingOptions readingOptions = optionHandler.readingOptions().withObjectFilter(CreatureListCommands::neededClasses);

      Stopwatch stopwatch = new Stopwatch(optionHandler.useStopwatch());
      saveFile = new ArkSavegame(savePath, readingOptions);
      stopwatch.stop("Reading");
      writeAnimalLists(filter);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void writeAnimalLists(BiPredicate<GameObject, ArkSavegame> filter) {
    Stream<GameObject> objectStream = saveFile.getObjects().parallelStream().filter(CreatureListCommands::onlyCreatures);

    if (filter != null) {
      objectStream = objectStream.filter(object -> filter.test(object, saveFile));
    }

    if (!options.has(untameableSpec)) {
      objectStream = objectStream.filter(CreatureListCommands::onlyTameable);
    }

    if (options.has(cleanFolderSpec)) {
      try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(outputDirectory, "*.json")) {
        for (Path path : directoryStream) {
          Files.delete(path);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    ConcurrentMap<String, List<GameObject>> dinoLists = objectStream.collect(Collectors.groupingByConcurrent(GameObject::getClassString));

    if (!options.has(withoutIndexSpec)) {
      Map<String, String> classNames = readClassNames();

      Function<String, String> fetchName = key -> DataManager.hasCreature(key) ? DataManager.getCreature(key).getName() : key;
      dinoLists.keySet().forEach(dinoClass -> classNames.computeIfAbsent(dinoClass, fetchName));

      writeClassNames(classNames);

      if (options.has(statisticsSpec)) {
        writerFunction = CreatureListCommands::writeEmptyWithStatistic;
      } else {
        writerFunction = CreatureListCommands::writeEmptyWithoutStatistic;
      }

      classNames.keySet().stream().filter(s -> !dinoLists.containsKey(s)).forEach(CreatureListCommands::writeEmpty);
    }

    dinoLists.entrySet().parallelStream().forEach(CreatureListCommands::writeList);
  }

  public static Map<String, String> readClassNames() {
    Path classFile = outputDirectory.resolve("classes.json");
    Map<String, String> classNames = new HashMap<>();

    if (Files.exists(classFile)) {
      
      try {
        CommonFunctions.readJson(classFile, parser -> {
          parser.nextToken();
          if (!parser.isExpectedStartArrayToken()) {
            return;
          }

          while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (!parser.isExpectedStartObjectToken()) {
              return;
            }

            String cls = null;
            String name = null;
            while (parser.nextValue() != JsonToken.END_OBJECT) {
              if ("cls".equals(parser.getCurrentName())) {
                cls = parser.getValueAsString();
              } else if ("name".equals(parser.getCurrentName())) {
                name = parser.getValueAsString();
              }
            }

            if (cls != null && name != null && !classNames.containsKey(cls)) {
              classNames.put(cls, name);
            }
          }
        });
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return classNames;
  }

  public static void writeClassNames(Map<String, String> classNames) {
    Path classFile = outputDirectory.resolve("classes.json");

    try {
      CommonFunctions.writeJson(classFile, generator -> {
        generator.writeStartArray();

        for (String cls: classNames.keySet()) {
          generator.writeStartObject();

          generator.writeStringField("cls", cls);
          generator.writeStringField("name", classNames.get(cls));

          generator.writeEndObject();
        }

        generator.writeEndArray();
      }, optionHandler);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void writeList(Map.Entry<String, List<GameObject>> entry) {
    Path outputFile = outputDirectory.resolve(entry.getKey() + ".json");

    List<? extends GameObject> filteredClasses = entry.getValue();
    LatLonCalculator latLongCalculator = LatLonCalculator.forSave(saveFile);

    try {
      CommonFunctions.writeJson(outputFile, generator -> {
        if (options.has(statisticsSpec)) {
          generator.writeStartObject();

          generator.writeNumberField("count", filteredClasses.size());

          IntSummaryStatistics statistics =
              filteredClasses.stream().filter(a -> CommonFunctions.onlyWild(a, saveFile)).mapToInt(a -> CommonFunctions.getBaseLevel(a, saveFile)).summaryStatistics();
          if (statistics.getCount() > 0) {
            generator.writeNumberField("wildMin", statistics.getMin());
            generator.writeNumberField("wildMax", statistics.getMax());
            generator.writeNumberField("wildAverage", statistics.getAverage());
          }

          IntSummaryStatistics tamedBaseStatistics =
              filteredClasses.stream().filter(a -> CommonFunctions.onlyTamed(a, saveFile)).mapToInt(a -> CommonFunctions.getBaseLevel(a, saveFile)).summaryStatistics();
          if (tamedBaseStatistics.getCount() > 0) {
            generator.writeNumberField("tamedBaseMin", tamedBaseStatistics.getMin());
            generator.writeNumberField("tamedBaseMax", tamedBaseStatistics.getMax());
            generator.writeNumberField("tamedBaseAverage", tamedBaseStatistics.getAverage());
          }

          IntSummaryStatistics tamedFullStatistics =
              filteredClasses.stream().filter(a -> CommonFunctions.onlyTamed(a, saveFile)).mapToInt(a -> CommonFunctions.getFullLevel(a, saveFile)).summaryStatistics();
          if (tamedFullStatistics.getCount() > 0) {
            generator.writeNumberField("tamedFullMin", tamedFullStatistics.getMin());
            generator.writeNumberField("tamedFullMax", tamedFullStatistics.getMax());
            generator.writeNumberField("tamedFullAverage", tamedFullStatistics.getAverage());
          }

          generator.writeArrayFieldStart("dinos");
        } else {
          generator.writeStartArray();
        }

        CustomDataContext context = new CustomDataContext();
        context.setLatLonCalculator(latLongCalculator);
        context.setObjectContainer(saveFile);
        for (GameObject creatureObject : filteredClasses) {
          Creature creature = new Creature(creatureObject, saveFile);
          generator.writeStartObject();
          creature.writeAllProperties(generator, context, options.has(writeAllFieldsSpec));
          if (options.has(inventorySpec)) {
            creature.writeInventory(generator, context, options.has(writeAllFieldsSpec), "summary".equals(options.valueOf(inventorySpec)));
          }
          generator.writeEndObject();
        }

        generator.writeEndArray();

        if (options.has(statisticsSpec)) {
          generator.writeEndObject();
        }
      }, optionHandler);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void writeEmptyWithStatistic(JsonGenerator generator) throws IOException {
    generator.writeStartObject();
    generator.writeNumberField("count", 0);
    generator.writeArrayFieldStart("dinos");
    generator.writeEndArray();
    generator.writeEndObject();
  }

  private static void writeEmptyWithoutStatistic(JsonGenerator generator) throws IOException {
    generator.writeStartArray();
    generator.writeEndArray();
  }

  private static void writeEmpty(String s) {
    Path outputFile = outputDirectory.resolve(s + ".json");

    try {
      CommonFunctions.writeJson(outputFile, writerFunction, optionHandler);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
