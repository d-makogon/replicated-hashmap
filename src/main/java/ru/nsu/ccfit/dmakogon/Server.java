package ru.nsu.ccfit.dmakogon;

import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import ru.nsu.ccfit.dmakogon.network.AppendEntriesRequest;
import ru.nsu.ccfit.dmakogon.network.AppendEntriesResponse;
import ru.nsu.ccfit.dmakogon.network.PeersNetwork;
import ru.nsu.ccfit.dmakogon.network.RaftRequest;
import ru.nsu.ccfit.dmakogon.network.RaftResponse;
import ru.nsu.ccfit.dmakogon.network.RequestVoteRequest;
import ru.nsu.ccfit.dmakogon.network.RequestVoteResponse;
import ru.nsu.ccfit.dmakogon.operations.Operation;
import ru.nsu.ccfit.dmakogon.operations.OperationType;
import ru.nsu.ccfit.dmakogon.storage.ReplicatedHashMapEntry;

@RequiredArgsConstructor
public class Server {
  private static final long TICK_TIME_MS = 1000;

  private final Thread workerThread = new Thread(this::serverRoutine);
  private long tickStartTime;
  private final PeersNetwork network;
  private final NodeState selfState;
  private final Peer selfPeer;
  private Peer leader;
  private final Random random = new Random();
  private int electionTimeout;
  private int selfVotesCount = 0;
  private final Consumer<Operation> applyLogListener;
  private final ServerSocket selfSocket;

  private final Timer electionTimer = new Timer("Election timer");
  private TimerTask electionTimerTask = null;
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

  private void resetElectionTimer() {
    if (electionTimerTask != null)
      electionTimerTask.cancel();
    electionTimerElapsed.set(false);
    var timeout = random.nextInt(150, 300);
    electionTimerTask = new TimerTask() {
      @Override
      public void run() {
        System.err.println("Election timer elapsed");
        electionTimerElapsed.set(true);
      }
    };
    electionTimer.schedule(electionTimerTask, timeout);
  }

  private void becomeFollower() {
    selfState.setType(NodeState.NodeType.FOLLOWER);
  }

  private void becomeCandidate() {
    selfState.setType(NodeState.NodeType.CANDIDATE);

    // On conversion to candidate, start election.
    startElection();
  }

  private void startElection() {
    // 1. Increment currentTerm
    long term = selfState.incrementCurrentTerm();

    // 2. Vote for self
    selfState.setVotedFor(selfPeer.getId());
    selfVotesCount = 1;

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
    HashMap
    System.err.println("sendHeartbeats");
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
    selfState.setType(NodeState.NodeType.LEADER);

    leader = selfPeer;

    // nextIndex and matchIndex are reinitialized after election.
    var log = selfState.getOperationsLog();
    for (var peer : selfState.getPeers()) {
      peer.setNextIndex(log.getLastIndex() + 1);
      peer.setMatchIndex(0);
    }

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
    // If follower receives an appendEntries request, it resets the election
    // timer.
    if (selfState.getType() == NodeState.NodeType.FOLLOWER && request.getType() == RaftRequest.RequestType.APPEND_ENTRIES)
      resetElectionTimer();

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
    var falseResponse = new AppendEntriesResponse(selfPeer, currentTerm, false,
                                                  log.getLastIndex()
    );
    // 1. Reply false if term < currentTerm (§5.1)
    if (request.getTerm() < currentTerm)
      return falseResponse;

    // If the leader’s term (included in its RPC) is at least as large as the
    // candidate’s current term, then the candidate recognizes the leader as
    // legitimate and returns to follower state.
    if (selfState.getType() == NodeState.NodeType.CANDIDATE)
      becomeFollower();

    // 2. Reply false if log doesn't contain an entry at prevLogIndex
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
    return new AppendEntriesResponse(selfPeer, currentTerm, true,
                                     log.getLastIndex()
    );
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
    var maybePeer = selfState.findPeer(response.getFromPeer().getId());
    if (maybePeer.isEmpty())
      throw new IllegalStateException("Unknown peer");
    var peer = maybePeer.get();
    if (response.isSuccess()) {
      int matchIndex = response.getMatchIndex();
      peer.setMatchIndex(matchIndex);
      peer.setNextIndex(matchIndex + 1);
      return;
    }
    // If AppendEntries fails because of log inconsistency: decrement
    // nextIndex and retry (§5.3)
    peer.decrementNextIndex();
  }

  private void handleRequestVoteResponse(RequestVoteResponse response) {
    if (selfState.getType() == NodeState.NodeType.CANDIDATE && response.isVoteGranted())
      selfVotesCount++;
  }

  private void applyLogEntry(Operation operation) {
    applyLogListener.accept(operation);
  }

  // If command received from client: append entry to local log, respond
  // after entry applied to state machine (§5.3)
  public void submit(OperationType type, ReplicatedHashMapEntry<?, ?> entry) {
    var log = selfState.getOperationsLog();
    log.append(new Operation(selfState.getCurrentTerm(), type, entry));
  }

  private void tryToCommit() {
    // If there exists an N such that N > commitIndex, a majority
    // of matchIndex[i] ≥ N, and operations[N].term == currentTerm:
    // set commitIndex = N (§5.3, §5.4).
    var log = selfState.getOperationsLog();
    while (true) {
      int N = selfState.getCommitIndex() + 1;
      Supplier<Long> peersWithHigherMatchIdx = () -> selfState.getPeers()
              .stream()
              .filter(peer -> peer.getMatchIndex() >= N)
              .count() + 1;
      if (log.getLastIndex() >= N && log.getTerm(
              N) == selfState.getCurrentTerm() && peersWithHigherMatchIdx.get() >= selfState.getQuorum())
        selfState.setCommitIndex(N);
      else
        return;
    }
  }

  private void sendAppendEntries() {
    // If last log index ≥ nextIndex for a follower: send AppendEntries
    // RPC with log entries starting at nextIndex.
    var log = selfState.getOperationsLog();
    int lastLogIndex = log.getLastIndex();
    for (var peer : selfState.getPeers()) {
      int nextIndex = peer.getNextIndex();
      if (lastLogIndex >= nextIndex) {
        var request = new AppendEntriesRequest(selfPeer,
                                               selfState.getCurrentTerm(),
                                               selfPeer.getId(), nextIndex,
                                               log.getTerm(nextIndex),
                                               log.allFromIndex(nextIndex),
                                               selfState.getCommitIndex()
        );
        network.sendRequest(peer, request);
      }
    }
  }

  private void handlePendingRequests() {
    while (true) {
      var maybeRequest = network.getNextRequest();
      if (maybeRequest.isEmpty())
        break;
      RaftRequest request = maybeRequest.get();
      var response = handleRequest(request);
      network.sendResponse(request.getFromPeer(), response);
    }
  }

  private void handlePendingResponses() {
    while (true) {
      var maybeResponse = network.getNextResponse();
      if (maybeResponse.isEmpty())
        break;
      handleResponse(maybeResponse.get());
    }
  }

  @SneakyThrows
  public void loop() {
    var channel = selfSocket.getChannel();
    var newSocketChannel = channel.accept();
    if (newSocketChannel != null) {
      newSocketChannel.configureBlocking(false);
      var newPeer = selfState.getPeers().createPeer();
      network.addPeer(newPeer, newSocketChannel.socket());
    }

    // If commitIndex > lastApplied: increment lastApplied, apply
    // log[lastApplied] to state machine (§5.3).
    int commitIndex = selfState.getCommitIndex();
    int lastApplied = selfState.getLastApplied();
    var log = selfState.getOperationsLog();
    if (commitIndex > lastApplied) {
      int newLastApplied = selfState.incrementLastApplied();
      applyLogEntry(log.get(newLastApplied));
    }

    handlePendingResponses();

    switch (selfState.getType()) {
      case LEADER -> {
        // Repeat during idle periods to prevent election timeouts (§5.2).
        sendHeartbeats();

        // If last log index ≥ nextIndex for a follower: send AppendEntries
        // RPC with log entries starting at nextIndex.
        sendAppendEntries();

        // If there exists an N such that N > commitIndex, a majority of
        // matchIndex[i] ≥ N, and log[N].term == currentTerm: set commitIndex
        // = N (§5.3, §5.4).
        tryToCommit();
      }
      case CANDIDATE -> {
        // If votes received from the majority of servers: become leader.
        // If election timeout elapses: start new election.
        if (selfVotesCount >= selfState.getQuorum())
          becomeLeader();
        else if (electionTimerElapsed.get())
          startElection();
      }
      case FOLLOWER -> {
        // Respond to RPCs from candidates and leaders.
        handlePendingRequests();

        // If election timeout elapses without receiving AppendEntries
        // RPC from current leader or granting vote to candidate:
        // convert to candidate.
        if (electionTimerElapsed.get())
          becomeCandidate();
      }
    }
  }

  private void serverRoutine() {
    int tick = 0;
    becomeFollower();
    resetElectionTimer();
    while (true) {
      System.err.println(
              "Tick " + tick + ", state: " + selfState.getType().toString());
      tick++;
      tickStartTime = System.currentTimeMillis();
      loop();
      long tickTime = System.currentTimeMillis() - tickStartTime;
      if (tickTime >= TICK_TIME_MS)
        continue;
      try {
        Thread.sleep(TICK_TIME_MS - tickTime);
      } catch (InterruptedException e) {
        break;
      }
    }
  }

  public void start() {
    workerThread.start();
  }

  public void stop() throws InterruptedException {
    workerThread.interrupt();
    workerThread.join();
  }
}
