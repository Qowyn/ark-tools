package qowyn.ark.tools.data;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.tools.LatLonCalculator;

public interface DataContext {

  public LatLonCalculator getLatLonCalculator();

  public GameObjectContainer getObjectContainer();

  public ArkSavegame getSavegame();

}
