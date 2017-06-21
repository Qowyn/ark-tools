package qowyn.ark.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qowyn.ark.tools.data.Item;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.LocationData;

public class TribeBase {

  private final String name;

  private final float x;

  private final float y;

  private final float z;

  private final float size;

  private final Map<ArkName, Integer> structures = new HashMap<>();

  private final Map<ArkName, Integer> creatures = new HashMap<>();

  private final List<Item> items = new ArrayList<>();

  private final List<Item> blueprints = new ArrayList<>();

  public TribeBase(String name, float x, float y, float z, float size) {
    this.name = name;
    this.x = x;
    this.y = y;
    this.z = z;
    this.size = size;
  }

  public String getName() {
    return name;
  }

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  public float getZ() {
    return z;
  }

  public float getSize() {
    return size;
  }

  public Map<ArkName, Integer> getStructures() {
    return structures;
  }

  public Map<ArkName, Integer> getCreatures() {
    return creatures;
  }

  public List<Item> getItems() {
    return items;
  }

  public List<Item> getBlueprints() {
    return blueprints;
  }

  @Override
  public int hashCode() {
    return (name == null) ? 0 : name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TribeBase other = (TribeBase) obj;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    return true;
  }

  public boolean insideBounds(LocationData location) {
    float diffX = x - location.getX();
    float diffY = y - location.getY();
    float diffZ = z - location.getZ();

    float distance = (float) Math.sqrt(Math.pow(diffX, 2) + Math.pow(diffY, 2) + Math.pow(diffZ, 2));

    return distance < size;
  }

  @Override
  public String toString() {
    return "TribeBase [name=" + name + ", x=" + x + ", y=" + y + ", z=" + z + ", size=" + size + "]";
  }
}
