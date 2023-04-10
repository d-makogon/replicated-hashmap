package ru.nsu.ccfit.dmakogon.network;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.nsu.ccfit.dmakogon.Peer;

@Getter
@Setter
@ToString
public class RequestVoteResponse extends RaftResponse {
  public static final RaftResponse.ResponseType SELF_TYPE =
          ResponseType.REQUEST_VOTE;

  private long term;
  private boolean voteGranted;

  public RequestVoteResponse(Peer from, long term, boolean voteGranted) {
    super(SELF_TYPE, from);
    this.term = term;
    this.voteGranted = voteGranted;
  }

  @Override
  public long getTerm() {
    return term;
  }

  public boolean isVoteGranted() {
    return voteGranted;
  }
}
