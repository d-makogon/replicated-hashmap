package ru.nsu.ccfit.dmakogon.operations;

import java.util.List;
import java.util.Optional;

public interface OperationsLog {
  void append(Operation operation);

  Optional<Operation> tryGet(int index);

  Operation get(int index);

  List<Operation> all();

  long getTerm(int index);

  int getLastIndex();

  long getLastTerm();

  void removeAllFromIndex(int index);

  List<Operation> allFromIndex(int index);
}
