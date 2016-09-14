package qowyn.ark.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue.ValueType;

import qowyn.ark.ArkSavegame;

/**
 * Provides calculation of ingame coordinates to lat and lon values
 * 
 * @author Roland Firmont
 *
 */
public final class LatLonCalculator {

  public static final Map<String, LatLonCalculator> knownMaps = new HashMap<>();

  /**
   * Default based on TheIsland
   */
  public static final LatLonCalculator defaultCalculator = new LatLonCalculator(50.0f, 8000.0f, 50.0f, 8000.0f);

  static {
    if (!importList()) {
      knownMaps.clear();
      knownMaps.put("TheIsland", defaultCalculator);
      knownMaps.put("TheCenter", new LatLonCalculator(30.34223747253418f, 9584.0f, 55.10416793823242f, 9600.0f));
      knownMaps.put("Valhalla", new LatLonCalculator(48.813560485839844f, 14750.0f, 48.813560485839844f, 14750.0f));
      knownMaps.put("MortemTupiu", new LatLonCalculator(32.479148864746094f, 20000.0f, 40.59893798828125f, 16000.0f));
      knownMaps.put("ShigoIslands", new LatLonCalculator(50.0f, 8128.0f, 50.0f, 8128.0f));
    }
  }

  /**
   * Exports current list of known maps as JsonObject.
   * 
   * @return list of known maps as JsonObject
   */
  public static JsonObject exportList() {
    JsonObjectBuilder builder = Json.createObjectBuilder();

    knownMaps.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
      JsonObjectBuilder entryBuilder = Json.createObjectBuilder();
      entryBuilder.add("latShift", entry.getValue().latShift);
      entryBuilder.add("latDiv", entry.getValue().latDiv);
      entryBuilder.add("lonShift", entry.getValue().lonShift);
      entryBuilder.add("lonDiv", entry.getValue().lonDiv);
      builder.add(entry.getKey(), entryBuilder);
    });

    return builder.build();
  }

  /**
   * @return true if latLonCalculator.json could be found and import was successful
   */
  private static boolean importList() {
    try (InputStream stream = LatLonCalculator.class.getResourceAsStream("/latLonCalculator.json")) {
      importList((JsonObject) CommonFunctions.readJson(stream));
      return true;
    } catch (FileNotFoundException e) {
      return false;
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(3);
      return false;
    }
  }

  /**
   * Imports list of known maps from JsonObject.
   * 
   * @param object list of known maps as a JsonObject
   */
  private static void importList(JsonObject object) {
    knownMaps.clear();
    try {
      object.forEach((mapName, entryValue) -> {
        if (entryValue.getValueType() != ValueType.OBJECT) {
          System.err.println("Error in provided LatLonCalculator settings: found non-object.");
          System.exit(3);
        }

        JsonObject entryObject = (JsonObject) entryValue;

        float latShift = entryObject.getJsonNumber("latShift").bigDecimalValue().floatValue();
        float latDiv = entryObject.getJsonNumber("latDiv").bigDecimalValue().floatValue();
        float lonShift = entryObject.getJsonNumber("lonShift").bigDecimalValue().floatValue();
        float lonDiv = entryObject.getJsonNumber("lonDiv").bigDecimalValue().floatValue();

        knownMaps.put(mapName, new LatLonCalculator(latShift, latDiv, lonShift, lonDiv));
      });
    } catch (RuntimeException ex) {
      System.err.println("Error in provided LatLonCalculator settings.");
      ex.printStackTrace();
      System.exit(3);
    }
  }

  /**
   * Tries to find the best match for the given {@code savegame}
   * 
   * @param savegame The savegame to find a LatLonCalculator for
   * @return a LatLonCalculator for the given {@code savegame} or {@link #defaultCalculator}
   */
  public static LatLonCalculator forSave(ArkSavegame savegame) {
    String mapName = savegame.getDataFiles().get(0);

    return knownMaps.getOrDefault(mapName, defaultCalculator);
  }

  private final float latShift;

  private final float latDiv;

  private final float lonShift;

  private final float lonDiv;

  public LatLonCalculator(float latShift, float latDiv, float lonShift, float lonDiv) {
    this.latShift = latShift;
    this.latDiv = latDiv;
    this.lonShift = lonShift;
    this.lonDiv = lonDiv;
  }

  public float calculateLat(float y) {
    return latShift + y / latDiv;
  }

  public float calculateLon(float x) {
    return lonShift + x / lonDiv;
  }

}
