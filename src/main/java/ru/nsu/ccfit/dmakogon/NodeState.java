package ru.nsu.ccfit.dmakogon;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.nsu.ccfit.dmakogon.network.PeersList;
import ru.nsu.ccfit.dmakogon.operations.OperationsLog;

@RequiredArgsConstructor
public class NodeState {
  public static final int NOT_VOTED_ID = -1;

  public enum NodeType {
    LEADER, CANDIDATE, FOLLOWER
  }

  @Getter
  @Setter
  private NodeType type = NodeType.CANDIDATE;

  private final AtomicLong currentTerm = new AtomicLong(0);
  private final AtomicInteger votedFor = new AtomicInteger(0);
  private final AtomicInteger commitIndex = new AtomicInteger(0);
  private final AtomicInteger lastApplied = new AtomicInteger(0);
  private final PeersList peers;
  private final OperationsLog operationsLog;

  public PeersList getPeers() {
    return peers;
  }

  public int getQuorum() {
    return (peers.size() + 1) / 2 + 1;
  }

  public OperationsLog getOperationsLog() {
    return operationsLog;
  }

  public long getCurrentTerm() {
    return currentTerm.get();
  }

  public boolean setCurrentTermToGreater(long term) {
    return currentTerm.updateAndGet(x -> Math.max(x, term)) < term;
  }

  public Optional<Peer> findPeer(int id) {
    var peersWithId = peers.stream()
            .filter(peer -> peer.getId() == id)
            .toList();
    if (peersWithId.size() > 1)
      throw new IllegalStateException("Several peers with same ids");
    if (peersWithId.size() == 1)
      return Optional.of(peersWithId.get(0));
    return Optional.empty();
  }

  public void setCurrentTerm(long currentTerm) {
    this.currentTerm.set(currentTerm);
  }

  public long incrementCurrentTerm() {
    return this.currentTerm.incrementAndGet();
  }

  public Optional<Integer> getVotedFor() {
    var value = votedFor.get();
    if (value == NOT_VOTED_ID)
      return Optional.empty();
    return Optional.of(value);
  }

  public void setVotedFor(int votedFor) {
    this.votedFor.set(votedFor);
  }

  public int getCommitIndex() {
    return commitIndex.get();
  }

  public int getLastApplied() {
    return lastApplied.get();
  }

  public int incrementLastApplied() {
    return lastApplied.incrementAndGet();
  }

  public void setCommitIndex(int commitIndex) {
    this.commitIndex.set(commitIndex);
  }
}
