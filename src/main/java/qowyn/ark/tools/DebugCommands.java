package qowyn.ark.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.json.stream.JsonGenerator;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.ReadingOptions;
import qowyn.ark.types.ArkName;

public class DebugCommands {

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

    try {
      String savePath = params.get(0);

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

      Consumer<JsonGenerator> writer = g -> {
        g.writeStartObject();

        g.write("_count", objects.size());

        map.entrySet().stream().sorted(Comparator.comparing(e -> e.getValue().size(), Comparator.reverseOrder())).forEach(e -> {
          String name = e.getKey();

          if (options.has(withNames)) {
            if (e.getValue().get(0).isItem()) {
              if (DataManager.hasItem(name)) {
                name = DataManager.getItem(name).getName();
              }
            } else {
              if (DataManager.hasCreature(name)) {
                name = DataManager.getCreature(name).getName();
              }
            }
          }

          g.write(name, e.getValue().size());
        });

        g.writeEnd();
      };

      if (params.size() > 1) {
        CommonFunctions.writeJson(params.get(1), writer, oh);
      } else {
        CommonFunctions.writeJson(System.out, writer, oh);
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
      String savePath = params.get(0);
      String className = params.get(1);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      Predicate<GameObject> filter = o -> o.getClassString().equals(className);

      ArkSavegame savegame = new ArkSavegame(savePath, oh.readingOptions().withObjectFilter(filter));

      stopwatch.stop("Loading");

      Consumer<JsonGenerator> dumpObjects = g -> {
        g.writeStartArray();

        savegame.getObjects().stream().filter(filter).forEach(o -> g.write(o.toJson(true)));

        g.writeEnd();
        g.flush();
      };

      if (params.size() > 2) {
        CommonFunctions.writeJson(params.get(2), dumpObjects, oh);
      } else {
        CommonFunctions.writeJson(System.out, dumpObjects, oh);
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
      String savePath = params.get(0);

      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      ArkSavegame savegame = new ArkSavegame(savePath, oh.readingOptions());

      stopwatch.stop("Loading");

      List<SizeObjectPair> pairList = savegame.getObjects().parallelStream()
          .map(SizeObjectPair::new)
          .sorted(Comparator.comparingInt(SizeObjectPair::getSize).reversed())
          .collect(Collectors.toList());

      stopwatch.stop("Collecting");

      Consumer<JsonGenerator> writer = g -> {
        g.writeStartArray();

        pairList.forEach(pair -> {
          g.writeStartObject();
          g.write("class", pair.object.getClassString());
          g.write("size", pair.size);
          g.writeEnd();
        });

        g.writeEnd();
        g.flush();
      };

      if (params.size() > 1) {
        CommonFunctions.writeJson(params.get(1), writer, oh);
      } else {
        CommonFunctions.writeJson(System.out, writer, oh);
      }

      stopwatch.stop("Writing");
      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static class SizeObjectPair {
    private final int size;
    private final GameObject object;

    public SizeObjectPair(GameObject object) {
      this.object = object;
      this.size = object.getSize(true) + object.getPropertiesSize(true);
    }

    public int getSize() {
      return size;
    }
  }

}
