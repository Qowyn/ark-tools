package qowyn.ark.tools.data;

import static qowyn.ark.tools.CommonFunctions.*;

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
import qowyn.ark.HibernationEntry;
import qowyn.ark.tools.GameObjectList;
import qowyn.ark.tools.LatLonCalculator;
import qowyn.ark.tools.ObjectCollector;
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

  public GameObjectContainer container;

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
    }).buildComponentTree(true));
    latLonCalculator = LatLonCalculator.forSave(savegame);

    if (!savegame.getHibernationEntries().isEmpty()) {
      List<GameObject> combinedObjects = new ArrayList<>(savegame.getObjects());

      for (HibernationEntry entry: savegame.getHibernationEntries()) {
        ObjectCollector collector = new ObjectCollector(entry, 1);
        combinedObjects.addAll(collector.remap(combinedObjects.size()));
      }

      container = new GameObjectList(combinedObjects);
    } else {
      container = savegame;
    }

    for (GameObject object: container) {
      if (object.isFromDataFile() || (object.getNames().size() == 1 && object.getNames().get(0).getInstance() == 0)) {
        // Skip things like NPCZoneVolume and non-instanced objects
      } else if (isInventory(object)) {
        inventoryMap.put(object.getId(), new Inventory(object));
      } else {
        if (!nameObjectMap.containsKey(object.getNames().get(0))) {
          nameObjectMap.put(object.getNames().get(0), object.getId());
          if (object.isItem()) {
            itemMap.put(object.getId(), new Item(object));
          } else if (isCreature(object)) {
            creatureMap.put(object.getId(), new Creature(object, savegame));
          } else if (object.getLocation() != null && !isPlayer(object) && !isDroppedItem(object) && !isWeapon(object)) {
            // Skip players, weapons and items on the ground
            // is (probably) a structure
            structureMap.put(object.getId(), new Structure(object, savegame));
          } else if (isDroppedItem(object)) {
            // dropped Item
            droppedItemMap.put(object.getId(), new DroppedItem(object, savegame));
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
    return container;
  }

  @Override
  public ArkSavegame getSavegame() {
    return savegame;
  }

}
