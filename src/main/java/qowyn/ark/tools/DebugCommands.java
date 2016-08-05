package qowyn.ark.tools;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.ReadingOptions;

public class DebugCommands {

  public static void classes(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage: classes <save>");
      return;
    }

    try {
      ArkSavegame savegame = new ArkSavegame(args[0], ReadingOptions.create().withObjectFilter(o -> false));

      ConcurrentMap<String, List<GameObject>> map = savegame.getObjects().parallelStream().collect(Collectors.groupingByConcurrent(GameObject::getClassString));

      map.entrySet().stream().sorted((e1, e2) -> -Integer.compare(e1.getValue().size(), e2.getValue().size())).forEach(e -> {
        System.out.println(e.getKey() + ": " + e.getValue().size());
      });
      System.out.println("Total: " + savegame.getObjects().size());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
