package qowyn.ark.tools;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.ReadingOptions;

public class DebugCommands {

  public static void classes(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 1 || oh.wantsHelp()) {
      System.out.println("Usage: ark-tools classes <save> [options]");
      oh.printHelp();
      System.exit(1);
      return;
    }

    try {
      String savePath = params.get(0);

      // Don't load any properties, we don't need them
      ArkSavegame savegame = new ArkSavegame(savePath, ReadingOptions.create().withObjectFilter(o -> false));

      ConcurrentMap<String, List<GameObject>> map = savegame.getObjects().parallelStream().collect(Collectors.groupingByConcurrent(GameObject::getClassString));

      // Reverse sort by count of objects
      map.entrySet().stream().sorted((e1, e2) -> -Integer.compare(e1.getValue().size(), e2.getValue().size())).forEach(e -> {
        System.out.println(e.getKey() + ": " + e.getValue().size());
      });
      System.out.println("Total: " + savegame.getObjects().size());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
