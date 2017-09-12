package qowyn.ark.tools.data;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.tools.LatLonCalculator;

public class CustomDataContext implements DataContext {

  private LatLonCalculator latLonCalculator;

  private GameObjectContainer objectContainer;

  private ArkSavegame savegame;

  public CustomDataContext() {}

  @Override
  public LatLonCalculator getLatLonCalculator() {
    return latLonCalculator;
  }

  public void setLatLonCalculator(LatLonCalculator latLonCalculator) {
    this.latLonCalculator = latLonCalculator;
  }

  @Override
  public GameObjectContainer getObjectContainer() {
    return objectContainer;
  }

  public void setObjectContainer(GameObjectContainer objectContainer) {
    this.objectContainer = objectContainer;
    if (objectContainer instanceof ArkSavegame) {
      this.savegame = (ArkSavegame) objectContainer;
    } else {
      this.savegame = null;
    }
  }

  @Override
  public ArkSavegame getSavegame() {
    return savegame;
  }

}
