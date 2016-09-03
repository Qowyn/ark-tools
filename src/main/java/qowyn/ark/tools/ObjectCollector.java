package qowyn.ark.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.PropertyContainer;
import qowyn.ark.arrays.ArkArray;
import qowyn.ark.properties.Property;
import qowyn.ark.properties.PropertyArray;
import qowyn.ark.properties.PropertyObject;
import qowyn.ark.properties.PropertyStruct;
import qowyn.ark.structs.Struct;
import qowyn.ark.types.ObjectReference;

public class ObjectCollector {

  private Map<Integer, GameObject> mappedObjects = new HashMap<>();

  public ObjectCollector(ArkSavegame saveFile, GameObject baseObject) {
    Deque<PropertyContainer> toVisit = new ArrayDeque<>();

    mappedObjects.put(baseObject.getId(), baseObject);
    toVisit.push(baseObject);

    while (!toVisit.isEmpty()) {
      PropertyContainer currentInstance = toVisit.pop();

      for (Property<?> property : currentInstance.getProperties()) {
        if (property instanceof PropertyObject) {
          PropertyObject po = (PropertyObject) property;
          ObjectReference reference = po.getValue();
          GameObject referenced = reference.getObject(saveFile);
          if (referenced != null && !mappedObjects.containsKey(referenced.getId())) {
            mappedObjects.put(referenced.getId(), referenced);
            toVisit.push(referenced);
          }
        } else if (property instanceof PropertyArray) {
          PropertyArray pa = (PropertyArray) property;
          ArkArray<Struct> structList = pa.getTypedValue(Struct.class);
          ArkArray<ObjectReference> objectReferenceList = pa.getTypedValue(ObjectReference.class);
          if (structList != null) {
            for (Struct struct : structList) {
              if (struct instanceof PropertyContainer) {
                toVisit.push((PropertyContainer) struct);
              }
            }
          } else if (objectReferenceList != null) {
            for (ObjectReference reference : objectReferenceList) {
              GameObject referenced = reference.getObject(saveFile);
              if (referenced != null && !mappedObjects.containsKey(referenced.getId())) {
                mappedObjects.put(referenced.getId(), referenced);
                toVisit.push(referenced);
              }
            }
          }
        } else if (property instanceof PropertyStruct) {
          PropertyStruct ps = (PropertyStruct) property;
          Struct struct = ps.getValue();
          if (struct instanceof PropertyContainer) {
            toVisit.push((PropertyContainer) struct);
          }
        }
      }
    }
  }

  /**
   * Grab all objects from save
   * @param saveFile
   */
  public ObjectCollector(ArkSavegame saveFile) {
    for (GameObject obj: saveFile.getObjects()) {
      mappedObjects.put(obj.getId(), obj);
    }
  }

  public Map<Integer, GameObject> getMappedObjects() {
    return mappedObjects;
  }

  public List<GameObject> remap(int startId) {
    List<GameObject> remappedList = new ArrayList<>(mappedObjects.values().size());

    applyOrderRules(remappedList);

    List<GameObject> alreadySorted = new ArrayList<>(remappedList);

    mappedObjects.values().stream().filter(go -> !alreadySorted.contains(go)).forEach(remappedList::add);

    for (int i = 0; i < remappedList.size(); i++) {
      remappedList.get(i).setId(startId + i);
    }

    remappedList.parallelStream().forEach(this::doRemap);

    return remappedList;
  }

  protected void applyOrderRules(List<GameObject> remappedList) {
    for (GameObject object : remappedList) {
      if (object.getClassString().contains("_Character_")) {
        // Dinos need to be defined before their components, might be related to GameObject#names
        remappedList.add(object);

        ObjectReference statusReference = object.getPropertyValue("MyCharacterStatusComponent", ObjectReference.class);

        if (statusReference != null) {
          remappedList.add(mappedObjects.get(statusReference.getObjectId()));
        }

        ObjectReference inventoryReference = object.getPropertyValue("MyInventoryComponent", ObjectReference.class);

        if (inventoryReference != null) {
          remappedList.add(mappedObjects.get(inventoryReference.getObjectId()));
        }
      }
    }
  }

  protected void doRemap(GameObject instance) {
    Deque<PropertyContainer> toVisit = new ArrayDeque<>();
    toVisit.push(instance);

    while (!toVisit.isEmpty()) {
      PropertyContainer currentInstance = toVisit.pop();
      for (Property<?> property : currentInstance.getProperties()) {
        if (property instanceof PropertyObject) {
          PropertyObject po = (PropertyObject) property;
          ObjectReference reference = po.getValue();
          if (reference.getObjectType() == 0 && reference.getObjectId() >= 0) {
            reference.setObjectId(mappedObjects.get(reference.getObjectId()).getId());
          }
        } else if (property instanceof PropertyArray) {
          PropertyArray pa = (PropertyArray) property;
          ArkArray<Struct> structList = pa.getTypedValue(Struct.class);
          ArkArray<ObjectReference> objectReferenceList = pa.getTypedValue(ObjectReference.class);
          if (structList != null) {
            for (Struct struct : structList) {
              if (struct instanceof PropertyContainer) {
                toVisit.push((PropertyContainer) struct);
              }
            }
          } else if (objectReferenceList != null) {
            for (ObjectReference reference : objectReferenceList) {
              if (reference.getObjectType() == 0 && reference.getObjectId() >= 0) {
                reference.setObjectId(mappedObjects.get(reference.getObjectId()).getId());
              }
            }
          }
        } else if (property instanceof PropertyStruct) {
          PropertyStruct ps = (PropertyStruct) property;
          Struct struct = ps.getValue();
          if (struct instanceof PropertyContainer) {
            toVisit.push((PropertyContainer) struct);
          }
        }
      }
    }
  }

}
