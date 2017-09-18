package qowyn.ark.tools;

import java.util.List;

import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;

public final class GameObjectList implements GameObjectContainer {

  private final List<GameObject> objects;

  public GameObjectList(List<GameObject> objects) {
    this.objects = objects;
  }

  @Override
  public List<GameObject> getObjects() {
    return objects;
  }

}
