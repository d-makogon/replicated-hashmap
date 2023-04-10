package ru.nsu.ccfit.dmakogon.network;

import ru.nsu.ccfit.dmakogon.Peer;

public interface RaftMessage {
  Peer getFromPeer();

  long getTerm();
}
