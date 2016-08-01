package qowyn.ark.tools;

import java.util.HashMap;
import java.util.Map;

import qowyn.ark.ArkSavegame;

public final class LatLonCalculator {

  public static final Map<String, LatLonCalculator> knownMaps = new HashMap<>();

  public static final LatLonCalculator defaultCalculator = new LatLonCalculator(50.0f, 8000.0f, 50.0f, 8000.0f);

  static {
    knownMaps.put("TheIsland", defaultCalculator);
    knownMaps.put("TheCenter", new LatLonCalculator(30.2922997f, 9584.0f, 55.054167f, 9600.0f));
  }

  public static LatLonCalculator forSave(ArkSavegame save) {
    String mapName = save.getDataFiles().get(0);

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
