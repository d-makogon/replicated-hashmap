package ru.nsu.ccfit.dmakogon;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import lombok.SneakyThrows;
import ru.nsu.ccfit.dmakogon.network.PeersList;
import ru.nsu.ccfit.dmakogon.network.PeersNetwork;
import ru.nsu.ccfit.dmakogon.network.PeersNetworkImpl;
import ru.nsu.ccfit.dmakogon.operations.InMemoryOperationsLog;
import ru.nsu.ccfit.dmakogon.operations.Operation;
import ru.nsu.ccfit.dmakogon.operations.OperationsLog;
import ru.nsu.ccfit.dmakogon.storage.InMemoryHashMap;
import ru.nsu.ccfit.dmakogon.storage.ReplicatedHashMap;
import sun.reflect.ReflectionFactory;

public class Program<K, V> implements AutoCloseable {
  private final PeersNetwork network;
  private final OperationsLog log;
  private final PeersList peers;
  private final SocketAddress selfAddress;
  private final List<InetSocketAddress> otherAddresses;
  private final List<Closeable> sockets = new ArrayList<>();
  private final ReplicatedHashMap<K, V> storage;
  private Server server;
  private int lastPeerId = 1;

  @SneakyThrows
  public Program(PeersNetwork network, OperationsLog log, PeersList peers,
                 ReplicatedHashMap<K, V> storage, SocketAddress address,
                 List<InetSocketAddress> otherAddresses) {
    this.network = network;
    this.log = log;
    this.peers = peers;
    this.storage = storage;
    this.selfAddress = address;
    this.otherAddresses = otherAddresses;
  }

//  @SneakyThrows
//  private Socket tryConnect(InetSocketAddress addr) {
//    System.err.println("Trying to connect to " + addr);
//    var socket = new Socket();
//    socket.connect(addr, 5000);
//    if (!socket.isConnected())
//      throw new IllegalArgumentException("Unknown host: %s".formatted(addr));
//    sockets.add(socket);
//    return socket;
//  }

  //  @SneakyThrows
//  private Peer addPeer(String host, int port) {
//    var peer = peers.addPeer();
//    var socket = tryConnect(host, port);
//    network.addPeer(peer, socket);
//    return peer;
//  }

  private void onApplyLogEntry(Operation operation) {
    var entry = operation.entry();
    switch (operation.type()) {
      case INSERT, UPDATE -> {
        storage.insert((K) entry.getKey(), (V) entry.getValue());
        System.out.printf("SET %s = %s\n", entry.getKey(), entry.getValue());
      }
      case DELETE -> {
        storage.remove((K) entry.getKey());
        System.out.printf("DELETE %s\n", entry.getKey());
      }
    }
  }

  @SneakyThrows
  public void run() {
    var selfNode = new NodeState(peers, log);
    var selfPeer = peers.createPeer(false);
    var channel = ServerSocketChannel.open();
    channel.bind(selfAddress);
    channel.configureBlocking(false);
    var socket = channel.socket();
    sockets.add(channel);
    for (var addr : otherAddresses) {
      var otherPeer = peers.createPeer();
      network.addPeerAddress(otherPeer, addr);
    }
    this.server = new Server(network, selfNode, selfPeer, this::onApplyLogEntry,
                             socket
    );
    server.start();
  }

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("Expected arg");
      return;
    }
    var scanner = new Scanner(args[0]);
    var isMainNode = scanner.nextBoolean();
    var network = new PeersNetworkImpl();
    var log = new InMemoryOperationsLog();
    var peers = new PeersList();
    var storage = new InMemoryHashMap<Integer, String>();
    var addr1 = new InetSocketAddress("localhost", 30000);
    var addr2 = new InetSocketAddress("localhost", 30001);
    InetSocketAddress selfAddress = addr1;
    InetSocketAddress otherAddress = addr2;
    if (!isMainNode) {
      selfAddress = addr2;
      otherAddress = addr1;
    }
    try (var program = new Program<>(network, log, peers, storage, selfAddress,
                                     List.of(otherAddress)
    )) {
      program.run();
      Thread.sleep(10000);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws Exception {
    server.stop();
    for (var socket : sockets)
      socket.close();
  }
}
