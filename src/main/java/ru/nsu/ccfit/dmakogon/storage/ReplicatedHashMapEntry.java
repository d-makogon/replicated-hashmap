package ru.nsu.ccfit.dmakogon.storage;

import java.util.AbstractMap;
import java.util.Map;

public class ReplicatedHashMapEntry<K, V> extends AbstractMap.SimpleEntry<K, V> {
  public ReplicatedHashMapEntry(K key, V value) {
    super(key, value);
  }

  public ReplicatedHashMapEntry(Map.Entry<? extends K, ? extends V> entry) {
    super(entry);
  }
}
