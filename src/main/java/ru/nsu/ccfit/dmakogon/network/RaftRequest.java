package ru.nsu.ccfit.dmakogon.network;

import lombok.Getter;
import ru.nsu.ccfit.dmakogon.Peer;

public abstract class RaftRequest implements RaftMessage {
  private final Peer fromPeer;

  public RaftRequest(RequestType type, Peer fromPeer) {
    this.type = type;
    this.fromPeer = fromPeer;
  }

  public enum RequestType {
    APPEND_ENTRIES, REQUEST_VOTE
  }

  @Getter
  private final RequestType type;

  @Override
  public Peer getFromPeer() {
    return fromPeer;
  }
}
