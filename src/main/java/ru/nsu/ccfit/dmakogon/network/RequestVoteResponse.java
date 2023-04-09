package ru.nsu.ccfit.dmakogon.network;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RequestVoteResponse extends RaftResponse {
  public static final RaftResponse.ResponseType SELF_TYPE =
          ResponseType.REQUEST_VOTE;

  private long term;
  private boolean voteGranted;

  public RequestVoteResponse(long term, boolean voteGranted) {
    super(SELF_TYPE);
    this.term = term;
    this.voteGranted = voteGranted;
  }
}
