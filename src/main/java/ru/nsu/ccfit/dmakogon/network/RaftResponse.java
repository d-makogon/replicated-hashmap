package ru.nsu.ccfit.dmakogon.network;

import lombok.Getter;

public abstract class RaftResponse implements RaftMessage {
  public RaftResponse(ResponseType type) {
    this.type = type;
  }

  public enum ResponseType {
    APPEND_ENTRIES, REQUEST_VOTE
  }

  @Getter
  private final ResponseType type;
}
