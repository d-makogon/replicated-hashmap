package ru.nsu.ccfit.dmakogon.network;

import java.util.ArrayList;

import ru.nsu.ccfit.dmakogon.Peer;

public class PeersList extends ArrayList<Peer> {
  private int lastPeerId = 0;

  public Peer createPeer() {
    return createPeer(true);
  }

  public Peer createPeer(boolean addToList) {
    var peer = new Peer(lastPeerId++);
    if (addToList)
      add(peer);
    return peer;
  }
}
