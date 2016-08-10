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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.ReadingOptions;
import qowyn.ark.properties.PropertyObject;
import qowyn.ark.types.ArkByteValue;
import qowyn.ark.types.LocationData;

public class AnimalListCommands {

  private static final Map<Integer, String> ATTRIBUTE_NAME_MAP = new HashMap<>();

  static {
    ATTRIBUTE_NAME_MAP.put(0, "health");
    ATTRIBUTE_NAME_MAP.put(1, "stamina");
    ATTRIBUTE_NAME_MAP.put(3, "oxygen");
    ATTRIBUTE_NAME_MAP.put(4, "food");
    ATTRIBUTE_NAME_MAP.put(7, "weight");
    ATTRIBUTE_NAME_MAP.put(8, "melee");
    ATTRIBUTE_NAME_MAP.put(9, "speed");
  }

  public static void animals(OptionHandler oh) {
    listImpl(oh, null);
  }

  public static void tamed(OptionHandler oh) {
    listImpl(oh, CommonFunctions::onlyTamed);
  }

  public static void wild(OptionHandler oh) {
    listImpl(oh, CommonFunctions::onlyWild);
  }

  protected static boolean neededClasses(GameObject object) {
    return object.getClassString().contains("_Character_") || object.getClassString().startsWith("DinoCharacterStatusComponent_");
  }

  protected static void listImpl(OptionHandler oh, Predicate<GameObject> filter) {
    try {
      List<String> params = oh.getParams();
      if (params.size() != 2 || oh.wantsHelp()) {
        System.out.println("Writes lists of all/tamed/wild animals in 'save' to the specified 'output_directory'.");
        System.out.println("Usage: ark-tools " + oh.getCommand() + " <save> <output_directory> [options]");
        oh.printHelp();
        System.exit(1);
        return;
      }

      String savePath = params.get(0);
      String outputDirectory = params.get(1);

      ReadingOptions options = oh.readingOptions()
          .withObjectFilter(AnimalListCommands::neededClasses);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());
      ArkSavegame saveFile = new ArkSavegame(savePath, options);
      stopwatch.stop("Reading");
      writeAnimalLists(outputDirectory, saveFile, filter, oh);
      stopwatch.stop("Dumping");

      stopwatch.print();
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void writeAnimalLists(String outputDirectory, ArkSavegame saveFile, Predicate<GameObject> filter, OptionHandler oh) {
    List<GameObject> objects;

    if (filter != null) {
      objects = saveFile.getObjects().stream().filter(filter).collect(Collectors.toList());
    } else {
      objects = saveFile.getObjects();
    }

    Map<String, String> classNames = readClassNames(outputDirectory);

    ConcurrentMap<String, List<GameObject>> dinoLists = objects.parallelStream()
        .filter(go -> go.getClassString().contains("_Character_"))
        .collect(Collectors.groupingByConcurrent(GameObject::getClassString));

    dinoLists.keySet().forEach(dinoClass -> classNames.putIfAbsent(dinoClass, dinoClass));

    writeClassNames(outputDirectory, classNames, oh);

    dinoLists.entrySet().parallelStream().forEach(e -> writeList(e, outputDirectory, saveFile, oh));

    classNames.keySet().stream().filter(s -> !dinoLists.containsKey(s)).forEach(s -> writeEmpty(s, outputDirectory, oh));
  }

  public static Map<String, String> readClassNames(String directory) {
    Path classFile = Paths.get(directory, "classes.json");
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

  public static void writeClassNames(String directory, Map<String, String> classNames, OptionHandler oh) {
    Path clsFile = Paths.get(directory, "classes.json");

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

  public static void writeList(Map.Entry<String, List<GameObject>> entry, String outputDirectory, ArkSavegame saveFile, OptionHandler oh) {
    Path outputFile = Paths.get(outputDirectory, entry.getKey() + ".json");

    List<? extends GameObject> filteredClasses = entry.getValue();
    LatLonCalculator latLongCalculator = LatLonCalculator.forSave(saveFile);

    try (OutputStream out = Files.newOutputStream(outputFile)) {
      CommonFunctions.writeJson(out, generator -> {
        generator.writeStartObject();

        IntSummaryStatistics statistics = filteredClasses.stream().mapToInt(a -> CommonFunctions.getBaseLevel(a, saveFile)).summaryStatistics();
        generator.write("count", statistics.getCount());
        if (statistics.getCount() > 0) {
          generator.write("min", statistics.getMin());
          generator.write("max", statistics.getMax());
          generator.write("average", statistics.getAverage());
        }

        IntSummaryStatistics fullLevelStatistics = filteredClasses.stream().filter(CommonFunctions::onlyTamed).mapToInt(a -> CommonFunctions.getFullLevel(a, saveFile)).summaryStatistics();
        if (fullLevelStatistics.getCount() > 0) {
          generator.write("tamedMin", fullLevelStatistics.getMin());
          generator.write("tamedMax", fullLevelStatistics.getMax());
          generator.write("tamedAverage", fullLevelStatistics.getAverage());
        }

        generator.writeStartArray("dinos");

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
          GameObject status = null;
          if (statusComp != null) {
            status = statusComp.getValue().getObject(saveFile);
          }

          if (status != null && status.getClassString().startsWith("DinoCharacterStatusComponent_")) {
            Integer baseLevel = status.getPropertyValue("BaseCharacterLevel", Integer.class);
            if (baseLevel != null) {
              generator.write("baseLevel", baseLevel);
            }

            if (baseLevel != null && baseLevel > 1) {
              generator.writeStartObject("wildLevels");
              for (Map.Entry<Integer, String> attribute : ATTRIBUTE_NAME_MAP.entrySet()) {
                ArkByteValue attrProp = status.getPropertyValue("NumberOfLevelUpPointsApplied", ArkByteValue.class, attribute.getKey());
                if (attrProp != null) {
                  generator.write(attribute.getValue(), attrProp.getByteValue());
                }
              }
              generator.writeEnd();
            }

            if (status.hasAnyProperty("NumberOfLevelUpPointsAppliedTamed")) {
              generator.writeStartObject("tamedLevels");
              for (Map.Entry<Integer, String> attribute : ATTRIBUTE_NAME_MAP.entrySet()) {
                ArkByteValue attrProp = status.getPropertyValue("NumberOfLevelUpPointsAppliedTamed", ArkByteValue.class, attribute.getKey());
                if (attrProp != null) {
                  generator.write(attribute.getValue(), attrProp.getByteValue());
                }
              }
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
        generator.writeEnd(); // Object
      }, oh);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void writeEmpty(String s, String outputDirectory, OptionHandler oh) {
    Path outputFile = Paths.get(outputDirectory, s + ".json");

    try (OutputStream out = Files.newOutputStream(outputFile)) {
      CommonFunctions.writeJson(out, g -> g.writeStartObject().write("count", 0).writeStartArray("dinos").writeEnd().writeEnd(), oh);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
