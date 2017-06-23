package qowyn.ark.tools.data;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.tools.LatLonCalculator;
import qowyn.ark.tools.OptionHandler;

public class DataCollector {

  private static final Pattern PROFILE_PATTERN = Pattern.compile("(\\d+|LocalPlayer)\\.arkprofile");

  private static final Pattern TRIBE_PATTERN = Pattern.compile("\\d+\\.arktribe");

  public final SortedMap<Integer, Item> itemMap = new TreeMap<>();

  public final SortedMap<Integer, Inventory> inventoryMap = new TreeMap<>();

  public final SortedMap<Integer, Creature> creatureMap = new TreeMap<>();

  public final SortedMap<Integer, Structure> structureMap = new TreeMap<>();

  public final SortedMap<Long, Player> playerMap = new TreeMap<>();

  public final SortedMap<Integer, Tribe> tribeMap = new TreeMap<>();

  public final OptionHandler oh;

  public ArkSavegame savegame;

  public LatLonCalculator latLonCalculator;

  public long maxPlayerAge;

  public boolean verbose;

  public DataCollector(OptionHandler oh) {
    this.oh = oh;
  }

  public void loadSavegame(Path path) throws IOException {
    savegame = new ArkSavegame(path.toString());
    latLonCalculator = LatLonCalculator.forSave(savegame);

    for (GameObject obj: savegame.getObjects()) {
      if (obj.isItem()) {
        itemMap.put(obj.getId(), new Item(obj));
      } else if (obj.getClassString().contains("Inventory")) {
        inventoryMap.put(obj.getId(), new Inventory(obj));
      } else if (obj.hasAnyProperty("MyCharacterStatusComponent") && !obj.hasAnyProperty("LinkedPlayerDataID")) {
        creatureMap.put(obj.getId(), new Creature(obj, savegame));
        // Skip players, weapons and items on the ground
      } else if (!obj.hasAnyProperty("LinkedPlayerDataID") && !obj.hasAnyProperty("AssociatedPrimalItem") && !obj.hasAnyProperty("MyItem")) {
        // is (probably) a structure
      }
    }
  }

  public void loadPlayers(Path path) throws IOException {
    Filter<Path> profileFilter = p -> PROFILE_PATTERN.matcher(p.getFileName().toString()).matches();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path, profileFilter)) {
      for (Path profilePath : stream) {
        if (maxPlayerAge != 0) {
          FileTime fileTime = Files.getLastModifiedTime(profilePath);

          if (fileTime.toInstant().isBefore(Instant.now().minusSeconds(maxPlayerAge))) {
            continue;
          }
        }

        try {
          Player player = new Player(profilePath, savegame, latLonCalculator);
          playerMap.put(player.playerDataId, player);
        } catch (RuntimeException ex) {
          System.err.println("Found potentially corrupt ArkProfile: " + profilePath.toString());
          if (verbose) {
            ex.printStackTrace();
          }
        }
      }
    }
  }

  public void loadTribes(Path path) {
    
  }

  public void loadCluster(Path path) {
    
  }

}
