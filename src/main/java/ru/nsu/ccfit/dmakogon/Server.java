package ru.nsu.ccfit.dmakogon;

import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import lombok.RequiredArgsConstructor;
import ru.nsu.ccfit.dmakogon.network.AppendEntriesRequest;
import ru.nsu.ccfit.dmakogon.network.AppendEntriesResponse;
import ru.nsu.ccfit.dmakogon.network.PeersNetwork;
import ru.nsu.ccfit.dmakogon.network.RaftRequest;
import ru.nsu.ccfit.dmakogon.network.RaftResponse;
import ru.nsu.ccfit.dmakogon.network.RequestVoteRequest;
import ru.nsu.ccfit.dmakogon.network.RequestVoteResponse;
import ru.nsu.ccfit.dmakogon.operations.Operation;

@RequiredArgsConstructor
public class Server {
  private final PeersNetwork network;
  private final NodeState selfState;
  private final Peer selfPeer;
  private Peer leader;
  private final Random random = new Random();
  private int electionTimeout;
  private final BiConsumer applyLogListener;

  private final Timer electionTimer = new Timer("Election timer");
  private final AtomicBoolean electionTimerElapsed = new AtomicBoolean(false);

  private void handleTypeChange(NodeState.NodeType newType) {
    switch (newType) {
      case LEADER -> becomeLeader();
      case CANDIDATE -> becomeCandidate();
      case FOLLOWER -> becomeFollower();
      default ->
              throw new IllegalStateException("Unexpected value: " + newType);
    }
  }

  // If election timeout elapses without receiving AppendEntries
  // RPC from current leader or granting vote to candidate:
  // convert to candidate.
  private void resetElectionTimer() {
    electionTimer.cancel();
    electionTimerElapsed.set(false);
    var newTimeout = random.nextInt(150, 300);
    electionTimer.schedule(new TimerTask() {
      @Override
      public void run() {
        electionTimerElapsed.set(true);
      }
    }, newTimeout);
  }

  private void becomeFollower() {
    selfState.setType(NodeState.NodeType.FOLLOWER);
  }

  private void becomeCandidate() {
    selfState.setType(NodeState.NodeType.CANDIDATE);

    // On conversion to candidate, start election:
    // 1. Increment currentTerm
    long term = selfState.incrementCurrentTerm();

    // 2. Vote for self
    selfState.setVotedFor(selfPeer.getId());

    // 3. Reset election timer
    resetElectionTimer();

    // 4. Send RequestVote RPCs to all other servers
    var log = selfState.getOperationsLog();
    var requestVote = new RequestVoteRequest(selfPeer, term, selfPeer.getId(),
                                             log.getLastIndex(),
                                             log.getLastTerm()
    );
    selfState.getPeers().forEach(peer -> {
      network.sendRequest(peer, requestVote);
    });
  }

  private void sendHeartbeats() {
    var log = selfState.getOperationsLog();
    int prevLogIndex = log.getLastIndex();
    var heartbeat = new AppendEntriesRequest(selfPeer,
                                             selfState.getCurrentTerm(),
                                             selfPeer.getId(), prevLogIndex,
                                             log.getTerm(prevLogIndex),
                                             List.of(),
                                             selfState.getCommitIndex()
    );
    selfState.getPeers().forEach(peer -> network.sendRequest(peer, heartbeat));
  }

  private void becomeLeader() {
    leader = selfPeer;
    // Upon election: send initial empty AppendEntries RPCs (heartbeat) to
    // each server; repeat during idle periods to prevent election timeouts
    // (§5.2)
    sendHeartbeats();
  }

  private void grantVote(int candidateId) {
    resetElectionTimer();
    selfState.setVotedFor(candidateId);
  }

  private RaftResponse handleRequest(RaftRequest request) {
    // If RPC request or response contains term T > currentTerm:
    // set currentTerm = T, convert to follower (§5.1)
    boolean hadOldTerm = selfState.setCurrentTermToGreater(request.getTerm());
    if (hadOldTerm)
      becomeFollower();

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
    // 1. Reply false if term < currentTerm (§5.1)
    var currentTerm = selfState.getCurrentTerm();
    if (request.getTerm() < currentTerm)
      return new RequestVoteResponse(selfPeer, currentTerm, false);

    // 2. If votedFor is null or candidateId, and candidate’s log is at
    //    least as up-to-date as receiver’s log, grant vote... (§5.2, §5.4)
    var votedFor = selfState.getVotedFor();
    var candidateId = request.getCandidateId();
    if (votedFor.isEmpty() || votedFor.get() == candidateId) {
      var lastLogTerm = request.getLastLogTerm();
      boolean hasGreaterTerm = lastLogTerm > currentTerm;
      boolean hasEqualTerm = lastLogTerm == currentTerm;
      boolean hasGreaterOrEqualIndex =
              request.getLastLogIndex() >= selfState.getLastApplied();
      if (hasGreaterTerm || (hasEqualTerm && hasGreaterOrEqualIndex)) {
        grantVote(candidateId);
        return new RequestVoteResponse(selfPeer, currentTerm, true);
      }
    }

    // ... else reply false.
    return new RequestVoteResponse(selfPeer, currentTerm, false);
  }

  private AppendEntriesResponse handleAppendEntriesRequest(
          AppendEntriesRequest request) {
    var log = selfState.getOperationsLog();
    var currentTerm = selfState.getCurrentTerm();
    var falseResponse = new AppendEntriesResponse(selfPeer, currentTerm, false);
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
    return new AppendEntriesResponse(selfPeer, currentTerm, true);
  }

  private void handleResponse(RaftResponse response) {
    // If RPC request or response contains term T > currentTerm:
    // set currentTerm = T, convert to follower (§5.1)
    boolean hadOldTerm = selfState.setCurrentTermToGreater(response.getTerm());
    if (hadOldTerm)
      becomeFollower();
    switch (response.getType()) {
      case APPEND_ENTRIES ->
              handleAppendEntriesResponse((AppendEntriesResponse) response);
      case REQUEST_VOTE ->
              handleRequestVoteResponse((RequestVoteResponse) response);
      default -> throw new IllegalStateException("Unexpected response type");
    }
  }

  private void handleAppendEntriesResponse(AppendEntriesResponse response) {
    // If successful: update nextIndex and matchIndex for follower (§5.3)
    // If AppendEntries fails because of log inconsistency: decrement
    // nextIndex and retry (§5.3)
  }

  private void handleRequestVoteResponse(RequestVoteResponse response) {
    //
  }

  private void applyLogEntry(Operation operation) {
    var entry = operation.entry();
    applyLogListener.accept(entry.getKey(), entry.getValue());
  }

  public void loop() {
    // If commitIndex > lastApplied: increment lastApplied, apply
    // log[lastApplied] to state machine (§5.3).
    int commitIndex = selfState.getCommitIndex();
    int lastApplied = selfState.getLastApplied();
    var log = selfState.getOperationsLog();
    if (commitIndex > lastApplied)
      applyLogEntry(log.get(lastApplied));

    while (true) {
      var maybeResponse = network.getNextResponse();
      if (maybeResponse.isEmpty())
        break;
      handleResponse(maybeResponse.get());
    }

    switch (selfState.getType()) {
      case LEADER -> {
      }
      case CANDIDATE -> {
      }
      case FOLLOWER -> {
        // Respond to RPCs from candidates and leaders
        while (true) {
          var maybeRequest = network.getNextRequest();
          if (maybeRequest.isEmpty())
            break;
          RaftRequest request = maybeRequest.get();
          if (request.getType() == RaftRequest.RequestType.APPEND_ENTRIES)
            resetElectionTimer();
          var response = handleRequest(request);
          network.sendResponse(request.getFromPeer(), response);
        }

        // If election timeout elapses without receiving AppendEntries
        // RPC from current leader or granting vote to candidate:
        // convert to candidate.
        if (electionTimerElapsed.get())
          becomeCandidate();
      }
    }
  }
}
