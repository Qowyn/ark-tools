package qowyn.ark.tools;

import static qowyn.ark.tools.CommonFunctions.iterable;
import static qowyn.ark.tools.CommonFunctions.writeJson;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkArchive;
import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.NameSizeCalculator;
import qowyn.ark.ReadingOptions;
import qowyn.ark.types.ArkName;

public class DebugCommands {

  private static final Comparator<Map.Entry<String, List<GameObject>>> SORT_BY_SIZE = Comparator.comparing(e -> e.getValue().size(), Comparator.reverseOrder());

  public static void classes(OptionHandler oh) {
    OptionSpec<Void> withoutDupesSpec = oh.accepts("without-dupes", "Removes duplicate objects");
    OptionSpec<Void> withNames = oh.accepts("with-names", "Write common name instead of class name where known.");

    OptionSet options = oh.reparse();

    List<String> params = oh.getParams(options);
    if (params.size() < 1 || params.size() > 2 || oh.wantsHelp()) {
      System.out.println("This command is primarily meant for debugging.");
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    if (options.has(withNames)) {
      DataManager.loadData(oh.lang());
    }

    try {
      Path savePath = Paths.get(params.get(0));

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      // Don't load any properties, we don't need them
      ArkSavegame savegame = new ArkSavegame(savePath, ReadingOptions.create().withObjectFilter(o -> false));

      stopwatch.stop("Loading");

      List<GameObject> objects;

      if (options.has(withoutDupesSpec)) {
        Set<ArkName> nameSet = new HashSet<>();
        objects = new ArrayList<>();

        for (GameObject object : savegame.getObjects()) {
          if (object.getNames().size() != 1) {
            objects.add(object);
          } else if (!nameSet.contains(object.getNames().get(0))) {
            objects.add(object);
            nameSet.add(object.getNames().get(0));
          }
        }
      } else {
        objects = savegame.getObjects();
      }

      ConcurrentMap<String, List<GameObject>> map = objects.parallelStream().collect(Collectors.groupingByConcurrent(GameObject::getClassString));

      stopwatch.stop("Grouping");

      WriteJsonCallback writer = generator -> {
        generator.writeStartObject();

        generator.writeNumberField("_count", objects.size());

        for (Map.Entry<String, List<GameObject>> entry: iterable(map.entrySet().stream().sorted(SORT_BY_SIZE))) {
          String name = entry.getKey();

          if (options.has(withNames)) {
            if (entry.getValue().get(0).isItem()) {
              if (DataManager.hasItem(name)) {
                name = DataManager.getItem(name).getName();
              }
            } else {
              if (DataManager.hasCreature(name)) {
                name = DataManager.getCreature(name).getName();
              } else if (DataManager.hasStructure(name)) {
                name = DataManager.getStructure(name).getName();
              }
            }
          }

          generator.writeNumberField(name, entry.getValue().size());
        }

        generator.writeEndObject();
      };

      if (params.size() > 1) {
        writeJson(Paths.get(params.get(1)), writer, oh);
      } else {
        writeJson(System.out, writer, oh);
      }

      stopwatch.stop("Writing");
      stopwatch.print();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void dump(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() < 2 || params.size() > 3 || oh.wantsHelp()) {
      System.out.println("This command is primarily meant for debugging.");
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      Path savePath = Paths.get(params.get(0));
      String className = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      Predicate<GameObject> filter = object -> object.getClassString().equals(className);

      ArkSavegame savegame = new ArkSavegame(savePath, oh.readingOptions().withObjectFilter(filter));

      stopwatch.stop("Loading");

      WriteJsonCallback dumpObjects = generator -> {
        generator.writeStartArray();

        for (GameObject object: savegame) {
          if (filter.test(object)) {
            object.writeJson(generator, true);
          }
        }

        generator.writeEndArray();
      };

      if (params.size() > 2) {
        writeJson(Paths.get(params.get(2)), dumpObjects, oh);
      } else {
        writeJson(System.out, dumpObjects, oh);
      }

      stopwatch.stop("Writing");
      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void sizes(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() < 1 || params.size() > 2 || oh.wantsHelp()) {
      System.out.println("This command is primarily meant for debugging.");
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      Path savePath = Paths.get(params.get(0));

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      ArkSavegame savegame = new ArkSavegame(savePath, oh.readingOptions());

      stopwatch.stop("Loading");

      List<SizeObjectPair> pairList = savegame.getObjects().parallelStream()
          .map(SizeObjectPair::new)
          .sorted(Comparator.comparingInt(SizeObjectPair::getSize).reversed())
          .collect(Collectors.toList());

      stopwatch.stop("Collecting");

      WriteJsonCallback writer = generator -> {
        generator.writeStartArray();

        for (SizeObjectPair pair: pairList) {
          generator.writeStartObject();
          generator.writeStringField("class", pair.object.getClassString());
          generator.writeNumberField("size", pair.size);
          generator.writeEndObject();
        }

        generator.writeEndObject();
      };

      if (params.size() > 1) {
        writeJson(Paths.get(params.get(1)), writer, oh);
      } else {
        writeJson(System.out, writer, oh);
      }

      stopwatch.stop("Writing");
      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class SizeObjectPair {
    private final static NameSizeCalculator NAME_SIZER = ArkArchive.getNameSizer(true);
    private final int size;
    private final GameObject object;

    public SizeObjectPair(GameObject object) {
      this.object = object;
      this.size = object.getSize(NAME_SIZER) + object.getPropertiesSize(NAME_SIZER);
    }

    public int getSize() {
      return size;
    }
  }

}
