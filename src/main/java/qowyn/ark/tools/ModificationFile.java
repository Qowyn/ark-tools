package qowyn.ark.tools;

import static qowyn.ark.tools.JsonValidator.expect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import qowyn.ark.tools.data.ArkItem;
import qowyn.ark.types.ArkName;

public class ModificationFile {

  public final Map<String, String> remapDinoClassName = new HashMap<>();

  public final Map<ArkName, ArkName> remapItemArchetype = new HashMap<>();

  public final Map<ArkName, List<ArkItem>> replaceInventories = new HashMap<>();

  public void readJson(JsonObject object) {
    JsonValue dinoClassNamesValue = object.get("dinoClassNames");

    if (dinoClassNamesValue != null) {
      if (expect(dinoClassNamesValue, "dinoClassNames", JsonValue.ValueType.OBJECT)) {
        JsonObject dinoClassNames = (JsonObject) dinoClassNamesValue;

        dinoClassNames.forEach((name, value) -> {
          if (expect(value, name, JsonValue.ValueType.STRING)) {
            remapDinoClassName.put(name, ((JsonString) value).getString());
          }
        });
      }
    }

    JsonValue itemArchetypesValue = object.get("itemArchetype");

    if (itemArchetypesValue != null) {
      if (expect(itemArchetypesValue, "itemArchetype", JsonValue.ValueType.OBJECT)) {
        JsonObject itemArchetypes = (JsonObject) itemArchetypesValue;

        itemArchetypes.forEach((name, value) -> {
          if (expect(value, name, JsonValue.ValueType.STRING)) {
            remapItemArchetype.put(new ArkName(name), new ArkName(((JsonString) value).getString()));
          }
        });
      }
    }

    
  }

}
