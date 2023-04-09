package ru.nsu.ccfit.dmakogon.operations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryOperationsLog implements OperationsLog {
  private static final int EMPTY_LOG_LAST_INDEX = -1;

  private final List<Operation> log = new CopyOnWriteArrayList<>();
  private final Object appendLock = new Object();

  @Override
  public void append(Operation operation) {
    synchronized (appendLock) {
      log.add(operation);
    }
  }

  @Override
  public Optional<Operation> tryGet(int index) {
    if (index >= log.size())
      return Optional.empty();
    return Optional.of(log.get(index));
  }

  @Override
  public Operation get(int index) {
    return log.get(index);
  }

  @Override
  public List<Operation> all() {
    return Collections.unmodifiableList(log);
  }

  @Override
  public long getTerm(int index) {
    if (index > EMPTY_LOG_LAST_INDEX)
      return log.get(index).term();
    return 0;
  }

  @Override
  public int getLastIndex() {
    return log.size() - 1;
  }

  @Override
  public long getLastTerm() {
    return getTerm(getLastIndex());
  }

  @Override
  public void removeAllFromIndex(int index) {
    synchronized (appendLock) {
      var toRemove = new ArrayList<Operation>();
      var size = log.size();
      for (int i = index; i < size; i++)
        toRemove.add(log.get(i));
      log.removeAll(toRemove);
    }
  }
}
