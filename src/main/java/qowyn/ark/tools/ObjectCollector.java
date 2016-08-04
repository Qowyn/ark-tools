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

  private GameObject baseObject;

  public ObjectCollector(ArkSavegame saveFile, GameObject baseObject) {
    this.baseObject = baseObject;
    Deque<PropertyContainer> toVisit = new ArrayDeque<>();

    mappedObjects.put(baseObject.getId(), baseObject);
    toVisit.push(baseObject);

    while (!toVisit.isEmpty()) {
      PropertyContainer currentInstance = toVisit.pop();

      for (Property<?> property : currentInstance.getProperties()) {
        if (property instanceof PropertyObject) {
          PropertyObject po = (PropertyObject) property;
          ObjectReference reference = po.getValue();
          if (reference.getObjectType() == 0 && !mappedObjects.containsKey(reference.getObjectId())) {
            GameObject referenced = saveFile.getObjects().get(reference.getObjectId());
            mappedObjects.put(referenced.getId(), referenced);
            toVisit.push(referenced);
          }
        } else if (property instanceof PropertyArray) {
          PropertyArray pa = (PropertyArray) property;
          ArkArray<PropertyContainer> propertyContainerList = pa.getTypedValue(PropertyContainer.class);
          ArkArray<ObjectReference> objectReferenceList = pa.getTypedValue(ObjectReference.class);
          if (propertyContainerList != null) {
            for (PropertyContainer container : propertyContainerList) {
              toVisit.push(container);
            }
          } else if (objectReferenceList != null) {
            for (ObjectReference reference : objectReferenceList) {
              if (reference.getObjectType() == 0 && !mappedObjects.containsKey(reference.getObjectId())) {
                GameObject referenced = saveFile.getObjects().get(reference.getObjectId());
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

  public Map<Integer, GameObject> getMappedObjects() {
    return mappedObjects;
  }

  public List<GameObject> remap(int startId) {
    List<GameObject> remappedList = new ArrayList<>(mappedObjects.values().size());

    // Make sure baseObject is the first Object, needed in case of dinos
    remappedList.add(baseObject);

    mappedObjects.values().stream().filter(go -> go != baseObject).forEach(remappedList::add);

    for (int i = 0; i < remappedList.size(); i++) {
      remappedList.get(i).setId(startId + i);
    }

    remappedList.parallelStream().forEach(this::doRemap);

    return remappedList;
  }

  public void doRemap(GameObject instance) {
    Deque<PropertyContainer> toVisit = new ArrayDeque<>();
    toVisit.push(instance);

    while (!toVisit.isEmpty()) {
      PropertyContainer currentInstance = toVisit.pop();
      for (Property<?> property : currentInstance.getProperties()) {
        if (property instanceof PropertyObject) {
          PropertyObject po = (PropertyObject) property;
          ObjectReference reference = po.getValue();
          if (reference.getObjectType() == 0) {
            reference.setObjectId(mappedObjects.get(reference.getObjectId()).getId());
          }
        } else if (property instanceof PropertyArray) {
          PropertyArray pa = (PropertyArray) property;
          ArkArray<PropertyContainer> propertyContainerList = pa.getTypedValue(PropertyContainer.class);
          ArkArray<ObjectReference> objectReferenceList = pa.getTypedValue(ObjectReference.class);
          if (propertyContainerList != null) {
            for (PropertyContainer container : propertyContainerList) {
              toVisit.push(container);
            }
          } else if (objectReferenceList != null) {
            for (ObjectReference reference : objectReferenceList) {
              if (reference.getObjectType() == 0) {
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
