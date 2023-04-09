package ru.nsu.ccfit.dmakogon;

import java.util.Timer;

import lombok.RequiredArgsConstructor;
import ru.nsu.ccfit.dmakogon.network.AppendEntriesRequest;
import ru.nsu.ccfit.dmakogon.network.AppendEntriesResponse;
import ru.nsu.ccfit.dmakogon.network.PeersNetwork;
import ru.nsu.ccfit.dmakogon.network.RaftRequest;
import ru.nsu.ccfit.dmakogon.network.RaftResponse;
import ru.nsu.ccfit.dmakogon.network.RequestVoteRequest;
import ru.nsu.ccfit.dmakogon.network.RequestVoteResponse;

@RequiredArgsConstructor
public class Server {
  private final PeersNetwork network;
  private final NodeState selfState;
  private final Peer selfPeer;
  private int electionTimeout;

  private final Timer electionTimer = new Timer("Election timer");

  private void handleTypeChange(NodeState.NodeType newType) {
    switch (newType) {
      case LEADER -> onBecomeLeader();
      case CANDIDATE -> onBecomeCandidate();
      case FOLLOWER -> onBecomeFollower();
      default ->
              throw new IllegalStateException("Unexpected value: " + newType);
    }
  }

  private void resetElectionTimer() {
    // TODO
    throw new RuntimeException("Not implemented");
  }

  private void onBecomeFollower() {
  }

  private void onBecomeCandidate() {
    long term = selfState.incrementCurrentTerm();

    selfState.setVotedFor(selfPeer.getId());

    resetElectionTimer();

    var requestVote = new RequestVoteRequest(term, selfPeer.getId(),
                                             selfState.getOperationsLog()
                                                     .getLastIndex(),
                                             selfState.getOperationsLog()
                                                     .getLastTerm()
    );
    selfState.getPeers().forEach(peer -> {
      network.sendRequest(peer, requestVote);
    });
//    TimerTask t;
//    t.run();
//    electionTimer.();
  }

  private void onBecomeLeader() {

  }

  private RaftResponse handleRequest(RaftRequest request) {
    // If RPC request or response contains term T > currentTerm:
    // set currentTerm = T, convert to follower (§5.1)
    boolean hadOldTerm = selfState.setCurrentTermToGreater(request.getTerm());
    if (hadOldTerm)
      selfState.setType(NodeState.NodeType.FOLLOWER);

    switch (request.getType()) {
      case APPEND_ENTRIES -> {
        return handleAppendEntriesRequest((AppendEntriesRequest) request);
      }
      case REQUEST_VOTE -> {
        return handleRequestVoteRequest((RequestVoteRequest) request);
      }
      default -> throw new IllegalStateException("Unexpected request type");
    }
  }

  private RequestVoteResponse handleRequestVoteRequest(
          RequestVoteRequest request) {
    var currentTerm = selfState.getCurrentTerm();
    if (request.getTerm() < currentTerm)
      return new RequestVoteResponse(currentTerm, false);

    var votedFor = selfState.getVotedFor();
    if (votedFor.isEmpty() || votedFor.get() == request.getCandidateId()) {
      boolean hasGreaterTerm = request.getLastLogTerm() > currentTerm;
      boolean hasEqualTerm = request.getLastLogTerm() == currentTerm;
      boolean hasGreaterOrEqualIndex =
              request.getLastLogIndex() >= selfState.getLastApplied();
      if (hasGreaterTerm || (hasEqualTerm && hasGreaterOrEqualIndex))
        return new RequestVoteResponse(currentTerm, true);
    }

    return new RequestVoteResponse(currentTerm, false);
  }

  private AppendEntriesResponse handleAppendEntriesRequest(
          AppendEntriesRequest request) {
    var log = selfState.getOperationsLog();
    var currentTerm = selfState.getCurrentTerm();
    var falseResponse = new AppendEntriesResponse(currentTerm, false);
    // 1. Reply false if term < currentTerm (§5.1)
    if (request.getTerm() < currentTerm)
      return falseResponse;

    // 2. Reply false if log doesn’t contain an entry at prevLogIndex
    //    whose term matches prevLogTerm (§5.3)
    var entry = log.tryGet(request.getPrevLogIndex());
    if (entry.isEmpty() || entry.get().term() != request.getPrevLogTerm())
      return falseResponse;

    // 3. If an existing entry conflicts with a new one (same index
    //    but different terms), delete the existing entry and all that
    //    follow it (§5.3)
    int newIndex = request.getPrevLogIndex();
    for (var newEntry : request.getEntries()) {
      newIndex++;
      if (newIndex <= log.getLastIndex() && newEntry.term() != log.getTerm(
              newIndex))
        log.removeAllFromIndex(newIndex);
      // 4. Append any new entries not already in the log
      if (newIndex <= log.getLastIndex())
        continue;
      log.append(newEntry);
    }

    // 5. If leaderCommit > commitIndex, set commitIndex =
    //    min(leaderCommit, index of last new entry)
    int leaderCommit = request.getLeaderCommit();
    if (leaderCommit > selfState.getCommitIndex())
      selfState.setCommitIndex(Math.min(leaderCommit, log.getLastIndex()));
    return new AppendEntriesResponse(currentTerm, true);
  }

  public void loop() {
//    If commitIndex > lastApplied: increment lastApplied, apply
//log[lastApplied] to state machine (§5.3) TODO
    switch (selfState.getType()) {
      case LEADER -> {
      }
      case CANDIDATE -> {
      }
      case FOLLOWER -> {
      }
    }
  }
}
