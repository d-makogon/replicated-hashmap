package ru.nsu.ccfit.dmakogon.network;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;

import ru.nsu.ccfit.dmakogon.Peer;

public interface PeersNetwork {
  void addPeerAddress(Peer peer, InetSocketAddress address);

  void addPeer(Peer peer, Socket socket);

  Optional<? extends RaftResponse> getNextResponse();

  Optional<? extends RaftRequest> getNextRequest();

  boolean sendRequest(Peer peer, RaftRequest request);

  boolean sendResponse(Peer peer, RaftResponse response);
}
