package ru.nsu.ccfit.dmakogon.network;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.nsu.ccfit.dmakogon.Peer;

@Getter
@Setter
@ToString
public class RequestVoteRequest extends RaftRequest {
  public static final RequestType SELF_TYPE = RequestType.REQUEST_VOTE;

  private long term;
  private int candidateId;
  private int lastLogIndex;
  private long lastLogTerm;

  public RequestVoteRequest(Peer from, long term, int candidateId,
                            int lastLogIndex, long lastLogTerm) {
    super(SELF_TYPE, from);
    this.term = term;
    this.candidateId = candidateId;
    this.lastLogIndex = lastLogIndex;
    this.lastLogTerm = lastLogTerm;
  }

  @Override
  public long getTerm() {
    return term;
  }
}
