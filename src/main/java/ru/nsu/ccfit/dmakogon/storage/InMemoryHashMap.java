package ru.nsu.ccfit.dmakogon.storage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class InMemoryHashMap<K, V> implements ReplicatedHashMap<K, V> {
  private final Map<K, V> map = new ConcurrentHashMap<>();

  @Override
  public V get(K key) {
    return map.get(key);
  }

  @Override
  public void insert(K key, V value) {
    map.put(key, value);
  }

  @Override
  public void update(K key, V value) {
    map.put(key, value);
  }

  @Override
  public void remove(K key) {
    map.remove(key);
  }

  @Override
  public List<ReplicatedHashMapEntry<K, V>> all() {
    return map.entrySet()
            .stream()
            .map(entry -> new ReplicatedHashMapEntry<>(entry.getKey(),
                                                       entry.getValue()
            ))
            .collect(Collectors.toList());
  }
}
