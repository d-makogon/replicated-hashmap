package ru.nsu.ccfit.dmakogon.storage;

import java.util.List;

public interface ReplicatedHashMap<K, V> {
  V get(K key);
  void insert(K key, V value);
  void update(K key, V value);
  void remove(K key);

  List<ReplicatedHashMapEntry<K, V>> all();
}
