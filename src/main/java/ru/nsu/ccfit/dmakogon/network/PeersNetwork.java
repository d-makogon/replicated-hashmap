package ru.nsu.ccfit.dmakogon.network;

import java.net.Socket;
import java.util.Optional;

import ru.nsu.ccfit.dmakogon.Peer;

public interface PeersNetwork {
  void addPeer(Peer peer, Socket socket);

  Optional<? extends RaftResponse> getNextResponse();

  Optional<? extends RaftRequest> getNextRequest();

  void sendRequest(Peer peer, RaftRequest request);

  void sendResponse(Peer peer, RaftResponse response);
}
