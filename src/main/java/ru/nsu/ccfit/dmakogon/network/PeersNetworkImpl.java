package ru.nsu.ccfit.dmakogon.network;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;

import lombok.SneakyThrows;
import ru.nsu.ccfit.dmakogon.Peer;

public class PeersNetworkImpl implements PeersNetwork {
  private static final Gson gson = new Gson();

  private final Map<Peer, Socket> sockets = new HashMap<>();
  private final Map<Socket, Peer> peers = new HashMap<>();
  private final Map<Peer, InetSocketAddress> peersAddresses = new HashMap<>();
  private final Queue<RaftRequest> receivedRequests = new ArrayDeque<>();
  private final Queue<RaftResponse> receivedResponses = new ArrayDeque<>();

  @Override
  public void addPeerAddress(Peer peer, InetSocketAddress address) {
    peersAddresses.put(peer, address);
  }

  @SneakyThrows
  @Override
  public void addPeer(Peer peer, Socket socket) {
    System.err.printf("Connected peer #%d on %s\n", peer.getId(),
                      socket.getInetAddress()
    );
    sockets.put(peer, socket);
    peers.put(socket, peer);
  }

  @SneakyThrows
  private void removePeer(Peer peer) {
    try (var socket = sockets.remove(peer)) {
      if (socket != null)
        peers.remove(socket);
    }
  }

  @SneakyThrows
  @Override
  public Optional<? extends RaftResponse> getNextResponse() {
    readNewMessages();
    if (receivedResponses.isEmpty())
      return Optional.empty();
    return Optional.of(receivedResponses.poll());
  }

  @SneakyThrows
  @Override
  public Optional<? extends RaftRequest> getNextRequest() {
    readNewMessages();
    if (receivedRequests.isEmpty())
      return Optional.empty();
    return Optional.of(receivedRequests.poll());
  }

  @SneakyThrows
  private void readNewMessages() {
    for (var entry : sockets.entrySet()) {
      var socket = entry.getValue();
      var stream = socket.getInputStream();
      if (stream.available() > 0)
        readNewMessage(stream, socket);
    }
  }

  @SneakyThrows
  private void readNewMessage(InputStream inputStream, Socket socket) {
    var reader = new InputStreamReader(inputStream);
    var message = gson.fromJson(reader, RaftMessage.class);
    var fromPeer = peers.get(socket);
    // Replace the received peer id with the one that our server has for the
    // sender socket. This is because other server may have assigned another
    // id to itself.
    message.getFromPeer().setId(fromPeer.getId());
    if (message instanceof RaftRequest)
      receivedRequests.add((RaftRequest) message);
    else if (message instanceof RaftResponse)
      receivedResponses.add((RaftResponse) message);
    else
      throw new IllegalStateException("Unexpected message type (or null)");
  }

  @SneakyThrows
  private void writeObject(Socket socket, Object object) {
    var writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream()));
    gson.toJson(object, writer);
    writer.flush();
  }

  @SneakyThrows
  private Socket connectToPeer(Peer peer) {
    InetSocketAddress peerAddr = peersAddresses.get(peer);
    var socket = new Socket();
    // TODO: Handle membership changes ($6)
    try {
      socket.connect(peerAddr);
      return socket;
    } catch (ConnectException e) {
      return null;
    }
  }

  @SneakyThrows
  private boolean establishConnection(Peer peer) {
    if (!sockets.containsKey(peer)) {
      var socket = connectToPeer(peer);
      addPeer(peer, socket);
    }
    if (sockets.containsKey(peer)) {
      var socket = sockets.get(peer);
      if (socket.isConnected() && !socket.isClosed())
        return true;
      removePeer(peer);
      return false;
    }
    return true;
  }

  @SneakyThrows
  @Override
  public boolean sendRequest(Peer peer, RaftRequest request) {
    System.err.printf("Sending request to peer #%d: %s\n", peer.getId(),
                      request
    );
    if (!establishConnection(peer))
      return false;
    writeObject(sockets.get(peer), request);
    return true;
  }

  @SneakyThrows
  @Override
  public boolean sendResponse(Peer peer, RaftResponse response) {
    System.err.printf("Sending response to peer #%d: %s\n", peer.getId(),
                      response
    );
    if (!establishConnection(peer))
      return false;
    writeObject(sockets.get(peer), response);
    return true;
  }
}
