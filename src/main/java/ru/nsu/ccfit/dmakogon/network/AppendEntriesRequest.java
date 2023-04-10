package ru.nsu.ccfit.dmakogon.network;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.nsu.ccfit.dmakogon.Peer;
import ru.nsu.ccfit.dmakogon.operations.Operation;

@Getter
@Setter
@ToString
public class AppendEntriesRequest extends RaftRequest {
  public static final RequestType SELF_TYPE = RequestType.APPEND_ENTRIES;

  private long term;
  private int leaderId;
  private int prevLogIndex;
  private long prevLogTerm;
  private List<Operation> entries;
  private int leaderCommit;

  public AppendEntriesRequest(Peer from, long term, int leaderId,
                              int prevLogIndex, long prevLogTerm,
                              List<Operation> entries, int leaderCommit) {
    super(SELF_TYPE, from);
    this.term = term;
    this.leaderId = leaderId;
    this.prevLogIndex = prevLogIndex;
    this.prevLogTerm = prevLogTerm;
    this.entries = entries;
    this.leaderCommit = leaderCommit;
  }

  @Override
  public long getTerm() {
    return term;
  }

  public boolean isHeartbeat() {
    return entries.isEmpty();
  }
}
