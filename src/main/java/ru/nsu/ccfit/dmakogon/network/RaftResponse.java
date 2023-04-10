package ru.nsu.ccfit.dmakogon.network;

import lombok.Getter;
import ru.nsu.ccfit.dmakogon.Peer;

public abstract class RaftResponse implements RaftMessage {
  private final Peer fromPeer;

  public RaftResponse(ResponseType type, Peer fromPeer) {
    this.type = type;
    this.fromPeer = fromPeer;
  }

  public enum ResponseType {
    APPEND_ENTRIES, REQUEST_VOTE
  }

  @Getter
  private final ResponseType type;

  @Override
  public Peer getFromPeer() {
    return fromPeer;
  }
}
