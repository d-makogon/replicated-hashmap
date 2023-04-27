package ru.nsu.ccfit.dmakogon;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

public class Peer {
  @Getter
  @Setter
  private int id;

  public Peer(int id) {
    this.id = id;
  }

  private final transient AtomicInteger nextIndex = new AtomicInteger(0);
  private final transient AtomicInteger matchIndex = new AtomicInteger(-1);
  private final transient AtomicBoolean voteGranted = new AtomicBoolean(false);

  int getNextIndex() {
    return nextIndex.get();
  }

  void setNextIndex(int index) {
    nextIndex.set(index);
  }

  int decrementNextIndex() {
    return nextIndex.decrementAndGet();
  }

  int getMatchIndex() {
    return matchIndex.get();
  }

  void setMatchIndex(int index) {
    matchIndex.set(index);
  }

  boolean getVoteGranted() {
    return voteGranted.get();
  }

  void setVoteGranted(boolean granted) {
    voteGranted.set(granted);
  }
}
