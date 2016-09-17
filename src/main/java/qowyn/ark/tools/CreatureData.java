package qowyn.ark.tools;

public class CreatureData {

  private final String name;

  private final String className;

  private final String blueprint;

  private final String packagePath;

  private final String category;

  public CreatureData(String name, String className, String blueprint, String packagePath, String category) {
    this.name = name;
    this.className = className;
    this.blueprint = blueprint;
    this.packagePath = packagePath;
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public String getClassName() {
    return className;
  }

  public String getBlueprint() {
    return blueprint;
  }

  public String getPackagePath() {
    return packagePath;
  }

  public String getCategory() {
    return category;
  }

  @Override
  public int hashCode() {
    return (className == null) ? 0 : className.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CreatureData other = (CreatureData) obj;
    if (className == null) {
      if (other.className != null)
        return false;
    } else if (!className.equals(other.className))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "ArkCreature [name=" + name + ", id=" + className + ", blueprint=" + blueprint + ", category=" + category + "]";
  }

}
