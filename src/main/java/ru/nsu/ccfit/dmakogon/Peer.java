package ru.nsu.ccfit.dmakogon;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.Getter;

public class Peer {
  @Getter
  private int id;

  private final AtomicInteger nextIndex = new AtomicInteger(0);
  private final AtomicInteger matchIndex = new AtomicInteger(-1);
  private final AtomicBoolean voteGranted = new AtomicBoolean(false);

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
