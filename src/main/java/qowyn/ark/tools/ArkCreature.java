package qowyn.ark.tools;

public class ArkCreature {

  private final String name;

  private final String id;

  private final String blueprint;

  private final String category;

  public ArkCreature(String name, String id, String blueprint, String category) {
    this.name = name;
    this.id = id;
    this.blueprint = blueprint;
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public String getId() {
    return id;
  }

  public String getBlueprint() {
    return blueprint;
  }

  public String getCategory() {
    return category;
  }

  @Override
  public int hashCode() {
    return (id == null) ? 0 : id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ArkCreature other = (ArkCreature) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "ArkCreature [name=" + name + ", id=" + id + ", blueprint=" + blueprint + ", category=" + category + "]";
  }

}
