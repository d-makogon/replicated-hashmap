package ru.nsu.ccfit.dmakogon.network;

import lombok.Getter;

public abstract class RaftRequest implements RaftMessage {
  public RaftRequest(RequestType type) {
    this.type = type;
  }

  public enum RequestType {
    APPEND_ENTRIES, REQUEST_VOTE
  }

  @Getter
  private final RequestType type;

  public abstract long getTerm();
}
