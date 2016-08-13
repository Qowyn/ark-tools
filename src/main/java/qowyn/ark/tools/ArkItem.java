package qowyn.ark.tools;

public class ArkItem {
  
  private final String name;
  
  private final String blueprint;
  
  private final String blueprintGeneratedClass;
  
  private final String category;
  
  public ArkItem(String name, String blueprint, String blueprintGeneratedClass, String category) {
    this.name = name;
    this.blueprint = blueprint;
    this.blueprintGeneratedClass = blueprintGeneratedClass;
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public String getBlueprint() {
    return blueprint;
  }
  
  public String getBlueprintGeneratedClass() {
    return blueprintGeneratedClass;
  }

  public String getCategory() {
    return category;
  }

  @Override
  public int hashCode() {
    return (blueprint == null) ? 0 : blueprint.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ArkItem other = (ArkItem) obj;
    if (blueprint == null) {
      if (other.blueprint != null)
        return false;
    } else if (!blueprint.equals(other.blueprint))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "ArkItem [name=" + name + ", blueprint=" + blueprint + ", blueprintGeneratedClass=" + blueprintGeneratedClass + ", category=" + category + "]";
  }

}
