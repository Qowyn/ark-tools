package qowyn.ark.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;

import qowyn.ark.ArkSavegame;

/**
 * Provides calculation of ingame coordinates to lat and lon values
 * 
 * @author Roland Firmont
 *
 */
public final class LatLonCalculator {

  public static final NavigableMap<String, LatLonCalculator> knownMaps = new TreeMap<>();

  /**
   * Default based on TheIsland
   */
  public static final LatLonCalculator DEFAULT = new LatLonCalculator(50.0f, 8000.0f, 50.0f, 8000.0f);

  static {
    if (!importList()) {
      knownMaps.clear();
      knownMaps.put("TheIsland", DEFAULT);
      knownMaps.put("TheCenter", new LatLonCalculator(30.34223747253418f, 9584.0f, 55.10416793823242f, 9600.0f));
      knownMaps.put("Valhalla", new LatLonCalculator(48.813560485839844f, 14750.0f, 48.813560485839844f, 14750.0f));
      knownMaps.put("MortemTupiu", new LatLonCalculator(32.479148864746094f, 20000.0f, 40.59893798828125f, 16000.0f));
      knownMaps.put("ShigoIslands", new LatLonCalculator(50.0f, 8128.0f, 50.0f, 8128.0f));
      knownMaps.put("Ragnarok", new LatLonCalculator(50.009388f, 13100f, 50.009388f, 13100f));
      knownMaps.put("TheVolcano", new LatLonCalculator(50.0f, 9181.0f, 50.0f, 9181.0f));
      knownMaps.put("PGARK", new LatLonCalculator(0.0f, 6080.0f, 0.0f, 6080.0f));
    }
  }

  /**
   * Exports current list of known maps
   * @param generator The generator to use
   */
  public static void exportList(JsonGenerator generator) throws IOException {
    generator.writeStartObject();

    for (String key: knownMaps.keySet()) {
      LatLonCalculator value = knownMaps.get(key);
      generator.writeObjectFieldStart(key);
      generator.writeNumberField("latShift", value.latShift);
      generator.writeNumberField("latDiv", value.latDiv);
      generator.writeNumberField("lonShift", value.lonShift);
      generator.writeNumberField("lonDiv", value.lonDiv);
      generator.writeEndObject();
    }

    generator.writeEndObject();
  }

  /**
   * @return true if latLonCalculator.json could be found and import was successful
   */
  private static boolean importList() {
    try (InputStream stream = LatLonCalculator.class.getResourceAsStream("/latLonCalculator.json")) {
      if (stream == null) {
        return false;
      }
      importList(CommonFunctions.readJson(stream));
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
   * Imports list of known maps from JsonNode.
   * 
   * @param node list of known maps as a JsonNode
   */
  private static void importList(JsonNode node) {
    knownMaps.clear();
    try {
      node.fields().forEachRemaining(entry -> {
        if (!entry.getValue().isObject()) {
          System.err.println("Error in provided LatLonCalculator settings: found non-object.");
          System.exit(3);
        }

        JsonNode entryNode = entry.getValue();

        float latShift = entryNode.path("latShift").floatValue();
        float latDiv = entryNode.path("latDiv").floatValue();
        float lonShift = entryNode.path("lonShift").floatValue();
        float lonDiv = entryNode.path("lonDiv").floatValue();

        knownMaps.put(entry.getKey(), new LatLonCalculator(latShift, latDiv, lonShift, lonDiv));
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
   * @return a LatLonCalculator for the given {@code savegame} or {@link #DEFAULT}
   */
  public static LatLonCalculator forSave(ArkSavegame savegame) {
    String mapName = savegame.getDataFiles().get(0);

    return knownMaps.getOrDefault(mapName, DEFAULT);
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
