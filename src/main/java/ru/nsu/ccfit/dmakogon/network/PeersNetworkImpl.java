package ru.nsu.ccfit.dmakogon.network;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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
  private final Queue<RaftRequest> receivedRequests = new ArrayDeque<>();
  private final Queue<RaftResponse> receivedResponses = new ArrayDeque<>();

  public PeersNetworkImpl() throws IOException {
  }

  @SneakyThrows
  @Override
  public void addPeer(Peer peer, Socket socket) {
    sockets.put(peer, socket);
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
        readNewMessage(stream);
    }
  }

  @SneakyThrows
  private void readNewMessage(InputStream inputStream) {
    var reader = new InputStreamReader(inputStream);
    var message = gson.fromJson(reader, RaftMessage.class);
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
  @Override
  public void sendRequest(Peer peer, RaftRequest request) {
    writeObject(sockets.get(peer), request);
  }

  @SneakyThrows
  @Override
  public void sendResponse(Peer peer, RaftResponse response) {
    writeObject(sockets.get(peer), response);
  }
}
