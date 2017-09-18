package qowyn.ark.tools;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.PropertyContainer;
import qowyn.ark.arrays.ArkArray;
import qowyn.ark.properties.Property;
import qowyn.ark.properties.PropertyArray;
import qowyn.ark.properties.PropertyObject;
import qowyn.ark.properties.PropertyStruct;
import qowyn.ark.structs.Struct;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.ObjectReference;

public final class ObjectCollector implements Iterable<GameObject> {

  private final Map<Integer, GameObject> mappedObjects = new LinkedHashMap<>();

  private final int startIndex;

  private int insertIndex;

  private int deleted;

  private int added;

  private boolean debug;

  public ObjectCollector(GameObjectContainer container, GameObject object, boolean followReferences, boolean withComponents) {
    Deque<PropertyContainer> toVisit = new ArrayDeque<>();
    startIndex = 0;

    mappedObjects.put(object.getId(), object);
    toVisit.push(object);

    visit(toVisit, container, followReferences, withComponents);

    insertIndex = mappedObjects.keySet().stream().max(Integer::compare).orElse(0);
  }

  public ObjectCollector(GameObjectContainer container, ArkName className, boolean followReferences, boolean withComponents) {
    Deque<PropertyContainer> toVisit = new ArrayDeque<>();
    startIndex = 0;

    for (GameObject object: container) {
      if (object.getClassName().equals(className)) {
        mappedObjects.put(object.getId(), object);
        toVisit.push(object);
      }
    }

    visit(toVisit, container, followReferences, withComponents);

    insertIndex = mappedObjects.keySet().stream().max(Integer::compare).orElse(0);
  }

  /**
   * Grab all objects from container
   * 
   * @param container
   */
  public ObjectCollector(GameObjectContainer container) {
    this(container, 0);
  }

  public ObjectCollector(GameObjectContainer container, int startIndex) {
    this.startIndex = startIndex;
    for (GameObject obj : container) {
      mappedObjects.put(obj.getId() + startIndex, obj);
    }
    insertIndex = mappedObjects.size() + startIndex;
  }

  private void visit(Deque<PropertyContainer> toVisit, GameObjectContainer container, boolean followReferences, boolean withComponents) {
    while (!toVisit.isEmpty()) {
      PropertyContainer currentInstance = toVisit.pop();

      if (followReferences) {
        for (Property<?> property : currentInstance.getProperties()) {
          if (property instanceof PropertyObject) {
            PropertyObject po = (PropertyObject) property;
            ObjectReference reference = po.getValue();
            GameObject referenced = reference.getObject(container);
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
                GameObject referenced = reference.getObject(container);
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

      if (withComponents && currentInstance instanceof GameObject) {
        GameObject object = (GameObject)currentInstance;

        for (GameObject component: object.getComponents().values()) {
          if (!mappedObjects.containsKey(component.getId())) {
            mappedObjects.put(component.getId(), component);
            toVisit.push(component);
          }
        }
      }
    }
  }

  public Map<Integer, GameObject> getMappedObjects() {
    return Collections.unmodifiableMap(mappedObjects);
  }

  public GameObject get(int index) {
    return mappedObjects.get(index);
  }

  public GameObject get(ObjectReference reference) {
    if (reference.isId() && reference.getObjectId() >= startIndex) {
      return mappedObjects.get(reference.getObjectId());
    }

    return null;
  }

  public boolean has(int index) {
    return mappedObjects.get(index) != null;
  }

  public boolean has(ObjectReference reference) {
    if (reference.isId() && reference.getObjectId() >= startIndex) {
      return mappedObjects.get(reference.getObjectId()) != null;
    }

    return false;
  }

  public void remove(int index) {
    GameObject removed = mappedObjects.remove(index);
    if (removed != null) {
      deleted++;
      if (debug) {
        System.out.println("Removed " + removed.getNames());
      }
    }
  }

  public void remove(GameObject object) {
    GameObject removed = mappedObjects.remove(object.getId());
    if (removed != null) {
      deleted++;
      if (debug) {
        System.out.println("Removed " + removed.getNames());
      }
    }
  }

  public void remove(ObjectReference reference) {
    if (reference.isId() && reference.getObjectId() >= startIndex) {
      GameObject removed = mappedObjects.remove(reference.getObjectId());
      if (removed != null) {
        deleted++;
        if (debug) {
          System.out.println("Removed " + removed.getNames());
        }
      }
    }
  }

  public int add(GameObject object) {
    object.setId(insertIndex);
    mappedObjects.put(insertIndex, object);

    added++;

    if (debug) {
      System.out.println("Added " + object.getNames());
    }

    return insertIndex++;
  }

  public List<GameObject> remap(int startId) {
    List<GameObject> remappedList = new ArrayList<>(mappedObjects.size());

    applyOrderRules(remappedList);

    for (int i = 0; i < remappedList.size(); i++) {
      remappedList.get(i).setId(startId + i);
    }

    remappedList.parallelStream().forEach(this::doRemap);

    return remappedList;
  }

  /**
   * Insert objects in the right order, ensures that components will be written after their owners
   * @param remappedList
   */
  protected void applyOrderRules(List<GameObject> remappedList) {
    Collection<GameObject> objects = mappedObjects.values();
    Map<Integer, Map<List<ArkName>, GameObject>> objectMap = new HashMap<>();

    // First step: clear all component information and collect names
    for (GameObject object : objects) {
      object.getComponents().clear();
      object.setParent(null);

      Integer mapKey = object.isFromDataFile() ? object.getDataFileIndex() : null;
      objectMap.computeIfAbsent(mapKey, key -> new HashMap<>()).putIfAbsent(object.getNames(), object);
    }

    // Second step: rebuild component information
    for (GameObject object : objects) {
      Map<List<ArkName>, GameObject> map = objectMap.get(object.isFromDataFile() ? object.getDataFileIndex() : null);
      if (object.hasParentNames() && map != null) {
        List<ArkName> targetName = object.getParentNames();

        GameObject parent = map.get(targetName);
        if (parent != null) {
          parent.addComponent(object);
          object.setParent(parent);
        }
      }
    }

    // Third step: build list by adding all objects without parent + their components
    Deque<GameObject> toVisit = new ArrayDeque<>();
    for (GameObject object : objects) {
      if (object.getParent() != null) {
        continue;
      }

      remappedList.add(object);

      toVisit.addAll(object.getComponents().values());

      while (!toVisit.isEmpty()) {
        GameObject current = toVisit.pop();

        remappedList.add(current);

        current.getComponents().values().forEach(toVisit::push);
      }
    }
  }

  /**
   * Refresh ObjectReferences, throws NPE for broken ObjectReferences.
   * An ObjectReference is considered broken if it's object has been deleted. 
   * @param instance
   */
  protected void doRemap(GameObject instance) {
    Deque<PropertyContainer> toVisit = new ArrayDeque<>();
    toVisit.push(instance);

    while (!toVisit.isEmpty()) {
      PropertyContainer currentInstance = toVisit.pop();
      for (Property<?> property : currentInstance.getProperties()) {
        if (property instanceof PropertyObject) {
          PropertyObject po = (PropertyObject) property;
          ObjectReference reference = po.getValue();
          if (reference.isId() && reference.getObjectId() >= startIndex) {
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
              if (reference.isId() && reference.getObjectId() >= startIndex) {
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

  @Override
  public Iterator<GameObject> iterator() {
    return mappedObjects.values().iterator();
  }

  public int getAdded() {
    return added;
  }

  public int getDeleted() {
    return deleted;
  }

  public boolean isDebug() {
    return debug;
  }

  public void setDebug(boolean debug) {
    this.debug = debug;
  }

}
