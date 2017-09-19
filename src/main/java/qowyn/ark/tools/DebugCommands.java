package qowyn.ark.tools;

import static qowyn.ark.tools.CommonFunctions.iterable;
import static qowyn.ark.tools.CommonFunctions.writeJson;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkArchive;
import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.HibernationEntry;
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
      boolean withoutDupes = options.has(withoutDupesSpec);

      // Don't load any properties, we don't need them
      ArkSavegame savegame = new ArkSavegame(savePath, ReadingOptions.create().withObjectFilter(o -> false).buildComponentTree(withoutDupes));

      stopwatch.stop("Loading");

      List<GameObject> objects;
      Map<Integer, Map<List<ArkName>, GameObject>> objectMap;

      if (!savegame.getHibernationEntries().isEmpty()) {
        objects = new ArrayList<>(savegame.getObjects());
        if (withoutDupes) {
          objectMap = new HashMap<>();
          savegame.getObjectMap().forEach((key, map) -> objectMap.put(key, new HashMap<>(map)));
        } else {
          objectMap = null;
        }

        for (HibernationEntry entry: savegame.getHibernationEntries()) {
          ObjectCollector collector = new ObjectCollector(entry, 1);
          objects.addAll(collector.remap(objects.size()));
          if (withoutDupes) {
            for (GameObject object: collector) {
              Integer key = object.isFromDataFile() ? object.getDataFileIndex() : null;
              objectMap.computeIfAbsent(key, k -> new HashMap<>()).putIfAbsent(object.getNames(), object);
            }
          }
        }
      } else {
        objects = savegame.getObjects();
        objectMap = savegame.getObjectMap();
      }

      if (withoutDupes) {
        objects.removeIf(object -> {
          Integer key = object.isFromDataFile() ? object.getDataFileIndex() : null;
          return objectMap.get(key).get(object.getNames()) != object;
        });
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

        for (HibernationEntry entry: savegame.getHibernationEntries()) {
          for (GameObject object: entry) {
            if (filter.test(object)) {
              object.writeJson(generator, true);
            }
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

      List<GameObject> hibernationObjects = new ArrayList<>();

      for (HibernationEntry entry: savegame.getHibernationEntries()) {
        hibernationObjects.addAll(entry.getObjects());
      }

      List<SizeObjectPair> pairList = Stream.concat(
          savegame.getObjects().parallelStream().map(object -> new SizeObjectPair(object, savegame.getSaveVersion() < 6, false)),
          hibernationObjects.parallelStream().map(object -> new SizeObjectPair(object, false, true)))
          .sorted(Comparator.comparingInt(SizeObjectPair::getSize).reversed())
          .collect(Collectors.toList());

      stopwatch.stop("Collecting");

      WriteJsonCallback writer = generator -> {
        generator.writeStartArray();

        for (SizeObjectPair pair: pairList) {
          generator.writeStartObject();
          generator.writeArrayFieldStart("names");
          for (ArkName name: pair.object.getNames()) {
            generator.writeString(name.toString());
          }
          generator.writeEndArray();
          if (pair.object.isFromDataFile()) {
            generator.writeNumberField("dataFileIndex", pair.object.getDataFileIndex());
          }
          generator.writeStringField("class", pair.object.getClassString());
          generator.writeNumberField("size", pair.size);
          generator.writeEndObject();
        }

        generator.writeEndArray();
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
    private final static NameSizeCalculator ANCIENT_SIZER = ArkArchive.getNameSizer(false);
    private final static NameSizeCalculator MODERN_SIZER = ArkArchive.getNameSizer(true);
    private final static NameSizeCalculator HIBERNATION_SIZER = ArkArchive.getNameSizer(true, true);
    private final int size;
    private final GameObject object;

    public SizeObjectPair(GameObject object, boolean ancient, boolean hibernation) {
      NameSizeCalculator nameSizer = hibernation ? HIBERNATION_SIZER : ancient ? ANCIENT_SIZER : MODERN_SIZER;
      this.object = object;
      this.size = object.getSize(nameSizer) + object.getPropertiesSize(nameSizer);
    }

    public int getSize() {
      return size;
    }
  }

}
