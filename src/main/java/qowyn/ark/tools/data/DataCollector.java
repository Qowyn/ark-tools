package qowyn.ark.tools.data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.tools.LatLonCalculator;
import qowyn.ark.tools.OptionHandler;
import qowyn.ark.types.ArkName;

public class DataCollector implements DataContext {

  private static final Pattern PROFILE_PATTERN = Pattern.compile("(\\d+|LocalPlayer)\\.arkprofile");

  private static final Pattern TRIBE_PATTERN = Pattern.compile("\\d+\\.arktribe");

  public final Map<ArkName, Integer> nameObjectMap = new HashMap<>();

  public final SortedMap<Integer, Item> itemMap = new TreeMap<>();

  public final SortedMap<Integer, DroppedItem> droppedItemMap = new TreeMap<>();

  public final SortedMap<Integer, Inventory> inventoryMap = new TreeMap<>();

  public final SortedMap<Integer, Creature> creatureMap = new TreeMap<>();

  public final SortedMap<Integer, Structure> structureMap = new TreeMap<>();

  public final SortedMap<Long, Player> playerMap;

  public final SortedMap<Long, ClusterStorage> playerClusterMap;

  public final SortedMap<Integer, Tribe> tribeMap;

  public final OptionHandler oh;

  public ArkSavegame savegame;

  public LatLonCalculator latLonCalculator;

  public long maxAge;

  private final List<Callable<Object>> tasks;

  public DataCollector(OptionHandler oh) {
    this.oh = oh;

    if (!oh.useParallel()) {
      tasks = null;
      playerMap = new TreeMap<>();
      playerClusterMap = new TreeMap<>();
      tribeMap = new TreeMap<>();
    } else {
      tasks = new ArrayList<>();
      playerMap = new ConcurrentSkipListMap<>();
      playerClusterMap = new ConcurrentSkipListMap<>();
      tribeMap = new ConcurrentSkipListMap<>();
    }
  }

  public void loadSavegame(Path path) throws IOException {
    savegame = new ArkSavegame(path, oh.readingOptions().withObjectFilter(obj -> {
      // Skip things like NPCZoneVolume and non-instanced objects
      return !obj.isFromDataFile() && (obj.getNames().size() > 1 || obj.getNames().get(0).getInstance() > 0);
    }));
    latLonCalculator = LatLonCalculator.forSave(savegame);

    for (GameObject obj: savegame.getObjects()) {
      if (obj.isFromDataFile() || (obj.getNames().size() == 1 && obj.getNames().get(0).getInstance() == 0)) {
        // Skip things like NPCZoneVolume and non-instanced objects
      } else if (obj.getClassString().contains("Inventory")) {
        inventoryMap.put(obj.getId(), new Inventory(obj));
      } else {
        if (!nameObjectMap.containsKey(obj.getNames().get(0))) {
          nameObjectMap.put(obj.getNames().get(0), obj.getId());
          if (obj.isItem()) {
            itemMap.put(obj.getId(), new Item(obj));
          } else if (obj.hasAnyProperty("MyCharacterStatusComponent") && !obj.hasAnyProperty("LinkedPlayerDataID")) {
            creatureMap.put(obj.getId(), new Creature(obj, savegame));
          } else if (obj.getLocation() != null && !obj.hasAnyProperty("LinkedPlayerDataID") && !obj.hasAnyProperty("AssociatedPrimalItem") && !obj.hasAnyProperty("MyItem") && !obj.hasAnyProperty("MyPawn")) {
            // Skip players, weapons and items on the ground
            // is (probably) a structure
            structureMap.put(obj.getId(), new Structure(obj, savegame));
          } else if (obj.hasAnyProperty("MyItem")) {
            // dropped Item
            droppedItemMap.put(obj.getId(), new DroppedItem(obj, savegame));
          }
        }
      }
    }
  }

  public void loadPlayers(Path path) throws IOException {
    Filter<Path> profileFilter = p -> PROFILE_PATTERN.matcher(p.getFileName().toString()).matches();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, profileFilter)) {
      for (Path profilePath : stream) {
        if (maxAge != 0) {
          FileTime fileTime = Files.getLastModifiedTime(profilePath);

          if (fileTime.toInstant().isBefore(Instant.now().minusSeconds(maxAge))) {
            continue;
          }
        }

        if (tasks != null) {
          tasks.add(Executors.callable(() -> loadPlayer(profilePath)));
        } else {
          loadPlayer(profilePath);
        }
      }
    }
  }

  protected void loadPlayer(Path profilePath) {
    try {
      Player player = new Player(profilePath, this, oh.readingOptions());
      playerMap.put(player.playerDataId, player);
    } catch (RuntimeException ex) {
      System.err.println("Found potentially corrupt ArkProfile: " + profilePath.toString());
      if (oh.isVerbose()) {
        ex.printStackTrace();
      }
    } catch (IOException ex) {
      if (oh.isVerbose()) {
        ex.printStackTrace();
      }
    }
  }

  public void loadTribes(Path path) throws IOException {
    Filter<Path> tribeFilter = p -> TRIBE_PATTERN.matcher(p.getFileName().toString()).matches();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, tribeFilter)) {
      for (Path tribePath : stream) {
        if (maxAge != 0) {
          FileTime fileTime = Files.getLastModifiedTime(tribePath);

          if (fileTime.toInstant().isBefore(Instant.now().minusSeconds(maxAge))) {
            continue;
          }
        }

        if (tasks != null) {
          tasks.add(Executors.callable(() -> loadTribe(tribePath)));
        } else {
          loadTribe(tribePath);
        }
      }
    }
  }

  protected void loadTribe(Path tribePath) {
    try {
      Tribe tribe = new Tribe(tribePath, oh.readingOptions());
      tribeMap.put(tribe.tribeId, tribe);
    } catch (RuntimeException ex) {
      System.err.println("Found potentially corrupt ArkTribe: " + tribePath.toString());
      if (oh.isVerbose()) {
        ex.printStackTrace();
      }
    } catch (IOException ex) {
      if (oh.isVerbose()) {
        ex.printStackTrace();
      }
    }
  }

  public void loadCluster(Path path) {
  }

  public void waitForData() {
    if (tasks != null) {
      ForkJoinPool pool = new ForkJoinPool(oh.threadCount(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
      pool.invokeAll(tasks);
      pool.shutdown();
      tasks.clear();
    }
  }

  @Override
  public LatLonCalculator getLatLonCalculator() {
    return latLonCalculator;
  }

  @Override
  public GameObjectContainer getObjectContainer() {
    return savegame;
  }

  @Override
  public ArkSavegame getSavegame() {
    return savegame;
  }

}
