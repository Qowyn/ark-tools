package qowyn.ark.tools;

import java.util.HashMap;
import java.util.Map;

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
    knownMaps.put("TheIsland", defaultCalculator);
    knownMaps.put("TheCenter", new LatLonCalculator(30.2922997f, 9584.0f, 55.054167f, 9600.0f));
  }

  /**
   * Tries to find the best match for the given {@code savegame}  
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
