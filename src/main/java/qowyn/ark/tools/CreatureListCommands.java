package qowyn.ark.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonGenerator;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.ReadingOptions;
import qowyn.ark.properties.PropertyObject;
import qowyn.ark.types.ArkByteValue;
import qowyn.ark.types.LocationData;

public class CreatureListCommands {

  private ArkSavegame saveFile;

  private OptionHandler oh;

  private String outputDirectory;

  private OptionSpec<Void> untameableSpec;

  private OptionSpec<Void> statisticsSpec;

  private OptionSet options;

  public CreatureListCommands(OptionHandler oh) {
    this.oh = oh;
  }

  public static void creatures(OptionHandler oh) {
    new CreatureListCommands(oh).listImpl(null);
  }

  public static void tamed(OptionHandler oh) {
    new CreatureListCommands(oh).listImpl(CommonFunctions::onlyTamed);
  }

  public static void wild(OptionHandler oh) {
    new CreatureListCommands(oh).listImpl(CommonFunctions::onlyWild);
  }

  protected static boolean neededClasses(GameObject object) {
    return object.getClassString().contains("_Character_") || object.getClassString().startsWith("DinoCharacterStatusComponent_");
  }

  protected static boolean onlyTameable(GameObject object) {
    return !object.hasAnyProperty("bForceDisablingTaming") || !object.getPropertyValue("bForceDisablingTaming", Boolean.class);
  }

  protected static boolean onlyCreatures(GameObject object) {
    return object.getClassString().contains("_Character_");
  }

  protected void listImpl(Predicate<GameObject> filter) {
    try {
      untameableSpec = oh.accepts("include-untameable", "Include untameable high-level dinos.");
      statisticsSpec = oh.accepts("statistics", "Wrap list of dinos in statistics block.");

      options = oh.reparse();

      List<String> params = oh.getParams(options);
      if (params.size() != 2 || oh.wantsHelp()) {
        oh.printCommandHelp();
        System.exit(1);
        return;
      }

      String savePath = params.get(0);
      outputDirectory = params.get(1);

      ReadingOptions readingOptions = oh.readingOptions()
          .withObjectFilter(CreatureListCommands::neededClasses);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      saveFile = new ArkSavegame(savePath, readingOptions);
      stopwatch.stop("Reading");
      writeAnimalLists(filter);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void writeAnimalLists(Predicate<GameObject> filter) {
    Stream<GameObject> objectStream = saveFile.getObjects().parallelStream().filter(CreatureListCommands::onlyCreatures);

    if (filter != null) {
      objectStream = objectStream.filter(filter);
    }

    if (!options.has(untameableSpec)) {
      objectStream = objectStream.filter(CreatureListCommands::onlyTameable);
    }

    Map<String, String> classNames = readClassNames();

    ConcurrentMap<String, List<GameObject>> dinoLists = objectStream.collect(Collectors.groupingByConcurrent(GameObject::getClassString));

    Function<String, String> fetchName = key -> DataManager.hasCreature(key) ? DataManager.getCreature(key).getName() : key;
    dinoLists.keySet().forEach(dinoClass -> classNames.computeIfAbsent(dinoClass, fetchName));

    writeClassNames(classNames);

    dinoLists.entrySet().parallelStream().forEach(this::writeList);

    classNames.keySet().stream().filter(s -> !dinoLists.containsKey(s)).forEach(this::writeEmpty);
  }

  public Map<String, String> readClassNames() {
    Path classFile = Paths.get(outputDirectory, "classes.json");
    Map<String, String> classNames = new HashMap<>();

    if (Files.exists(classFile)) {
      try (InputStream classStream = Files.newInputStream(classFile)) {
        JsonReader classReader = Json.createReader(classStream);
        JsonArray classArray = classReader.readArray();
        for (JsonObject o : classArray.getValuesAs(JsonObject.class)) {
          String cls = o.getString("cls");
          String name = o.getString("name");
          if (!classNames.containsKey(cls)) {
            classNames.put(cls, name);
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return classNames;
  }

  public void writeClassNames(Map<String, String> classNames) {
    Path clsFile = Paths.get(outputDirectory, "classes.json");

    try (OutputStream clsStream = Files.newOutputStream(clsFile)) {
      CommonFunctions.writeJson(clsStream, g -> {
        g.writeStartArray();

        classNames.forEach((cls, name) -> g.writeStartObject().write("cls", cls).write("name", name).writeEnd());

        g.writeEnd();
      }, oh);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void writeList(Map.Entry<String, List<GameObject>> entry) {
    Path outputFile = Paths.get(outputDirectory, entry.getKey() + ".json");

    List<? extends GameObject> filteredClasses = entry.getValue();
    LatLonCalculator latLongCalculator = LatLonCalculator.forSave(saveFile);

    try (OutputStream out = Files.newOutputStream(outputFile)) {
      CommonFunctions.writeJson(out, generator -> {
        if (options.has(statisticsSpec)) {
          generator.writeStartObject();

          generator.write("count", filteredClasses.size());

          IntSummaryStatistics statistics = filteredClasses.stream().filter(CommonFunctions::onlyWild).mapToInt(a -> CommonFunctions.getBaseLevel(a, saveFile)).summaryStatistics();
          if (statistics.getCount() > 0) {
            generator.write("wildMin", statistics.getMin());
            generator.write("wildMax", statistics.getMax());
            generator.write("wildAverage", statistics.getAverage());
          }

          IntSummaryStatistics tamedBaseStatistics = filteredClasses.stream().filter(CommonFunctions::onlyTamed).mapToInt(a -> CommonFunctions.getBaseLevel(a, saveFile)).summaryStatistics();
          if (tamedBaseStatistics.getCount() > 0) {
            generator.write("tamedBaseMin", tamedBaseStatistics.getMin());
            generator.write("tamedBaseMax", tamedBaseStatistics.getMax());
            generator.write("tamedBaseAverage", tamedBaseStatistics.getAverage());
          }

          IntSummaryStatistics tamedFullStatistics = filteredClasses.stream().filter(CommonFunctions::onlyTamed).mapToInt(a -> CommonFunctions.getFullLevel(a, saveFile)).summaryStatistics();
          if (tamedFullStatistics.getCount() > 0) {
            generator.write("tamedFullMin", tamedFullStatistics.getMin());
            generator.write("tamedFullMax", tamedFullStatistics.getMax());
            generator.write("tamedFullAverage", tamedFullStatistics.getAverage());
          }

          generator.writeStartArray("dinos");
        } else {
          generator.writeStartArray();
        }

        for (GameObject i : filteredClasses) {
          generator.writeStartObject();

          LocationData ld = i.getLocation();
          if (ld != null) {
            generator.write("x", ld.getX());
            generator.write("y", ld.getY());
            generator.write("z", ld.getZ());
            generator.write("lat", Math.round(latLongCalculator.calculateLat(ld.getY()) * 10.0) / 10.0);
            generator.write("lon", Math.round(latLongCalculator.calculateLon(ld.getX()) * 10.0) / 10.0);
          }

          if (i.hasAnyProperty("bIsFemale")) {
            generator.write("female", true);
          }

          if (i.hasAnyProperty("TamedAtTime")) {
            generator.write("tamed", true);
            generator.write("tamedTime", saveFile.getGameTime() - i.getPropertyValue("TamedAtTime", Double.class));
          }

          String tribeName = i.getPropertyValue("TribeName", String.class);
          if (tribeName != null) {
            generator.write("tribe", tribeName);
          }

          String tamerName = i.getPropertyValue("TamerString", String.class);
          if (tamerName != null) {
            generator.write("tamer", tamerName);
          }

          String name = i.getPropertyValue("TamedName", String.class);
          if (name != null) {
            generator.write("name", name);
          }

          String imprinter = i.getPropertyValue("ImprinterName", String.class);
          if (imprinter != null) {
            generator.write("imprinter", imprinter);
          }

          PropertyObject statusComp = i.getTypedProperty("MyCharacterStatusComponent", PropertyObject.class);
          GameObject status;
          if (statusComp != null) {
            status = statusComp.getValue().getObject(saveFile);
          } else {
            status = null;
          }

          if (status != null && status.getClassString().startsWith("DinoCharacterStatusComponent_")) {
            Integer baseLevel = status.getPropertyValue("BaseCharacterLevel", Integer.class);
            if (baseLevel != null) {
              generator.write("baseLevel", baseLevel);
            }

            if (baseLevel != null && baseLevel > 1) {
              generator.writeStartObject("wildLevels");
              AttributeNames.forEach((index, attrName) -> {
                ArkByteValue attrProp = status.getPropertyValue("NumberOfLevelUpPointsApplied", ArkByteValue.class, index);
                if (attrProp != null) {
                  generator.write(attrName, attrProp.getByteValue());
                }
              });
              generator.writeEnd();
            }

            Short extraLevel = status.getPropertyValue("ExtraCharacterLevel", Short.class);
            if (baseLevel != null && extraLevel != null) {
              generator.write("fullLevel", extraLevel + baseLevel);
            }

            if (status.hasAnyProperty("NumberOfLevelUpPointsAppliedTamed")) {
              generator.writeStartObject("tamedLevels");
              AttributeNames.forEach((index, attrName) -> {
                ArkByteValue attrProp = status.getPropertyValue("NumberOfLevelUpPointsAppliedTamed", ArkByteValue.class, index);
                if (attrProp != null) {
                  generator.write(attrName, attrProp.getByteValue());
                }
              });
              generator.writeEnd();
            }

            Float experience = status.getPropertyValue("ExperiencePoints", Float.class);
            if (experience != null) {
              generator.write("experience", experience);
            }

            Float imprintingQuality = status.getPropertyValue("DinoImprintingQuality", Float.class);
            if (imprintingQuality != null) {
              generator.write("imprintingQuality", imprintingQuality);
            }
          }

          generator.writeEnd();
        }

        generator.writeEnd(); // Array

        if (options.has(statisticsSpec)) {
          generator.writeEnd(); // Object
        }
      }, oh);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void writeEmpty(String s) {
    Path outputFile = Paths.get(outputDirectory, s + ".json");

    try (OutputStream out = Files.newOutputStream(outputFile)) {
      Consumer<JsonGenerator> writerFunction;
      if (options.has(statisticsSpec)) {
        writerFunction = g -> g.writeStartObject().write("count", 0).writeStartArray("dinos").writeEnd().writeEnd();
      } else {
        writerFunction = g -> g.writeStartArray().writeEnd();
      }

      CommonFunctions.writeJson(out, writerFunction, oh);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
