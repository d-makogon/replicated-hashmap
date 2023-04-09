package ru.nsu.ccfit.dmakogon.network;

import java.nio.channels.SocketChannel;
import java.util.Optional;

import ru.nsu.ccfit.dmakogon.Peer;

public interface PeersNetwork {
  void addPeer(Peer peer, SocketChannel socket);

  Optional<? extends RaftResponse> getNextResponse();

  Optional<? extends RaftRequest> getNextRequest();

  void sendRequest(Peer peer, RaftRequest request);
}
