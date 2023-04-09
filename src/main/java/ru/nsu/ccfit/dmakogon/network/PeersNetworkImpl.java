package ru.nsu.ccfit.dmakogon.network;

import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
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
  private final Selector selector = Selector.open();
  private final Queue<RaftRequest> receivedRequests = new ArrayDeque<>();
  private final Queue<RaftResponse> receivedResponses = new ArrayDeque<>();

  public PeersNetworkImpl() throws IOException {
  }

  @SneakyThrows
  @Override
  public void addPeer(Peer peer, SocketChannel channel) {
    sockets.put(peer, channel.socket());
    channel.register(selector, SelectionKey.OP_READ);
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
    selector.select();
    var selectedKeys = selector.selectedKeys();
    var iter = selectedKeys.iterator();
    while (iter.hasNext()) {
      SelectionKey key = iter.next();
      if (key.isReadable())
        readNewMessage((SocketChannel) key.channel());
      iter.remove();
    }
  }

  @SneakyThrows
  private void readNewMessage(SocketChannel channel) {
    var socket = channel.socket();
    var reader = new InputStreamReader(socket.getInputStream());
    var message = gson.fromJson(reader, RaftMessage.class);
    if (message instanceof RaftRequest)
      receivedRequests.add((RaftRequest) message);
    else if (message instanceof RaftResponse)
      receivedResponses.add((RaftResponse) message);
    else
      throw new IllegalStateException("Unexpected message type (or null)");
  }

  @SneakyThrows
  @Override
  public void sendRequest(Peer peer, RaftRequest request) {
    var socket = sockets.get(peer);
    var writer = new BufferedWriter(
            new OutputStreamWriter(socket.getOutputStream()));
    gson.toJson(request, writer);
    writer.flush();
  }
}
