package ru.nsu.ccfit.dmakogon.network;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.nsu.ccfit.dmakogon.Peer;

@Getter
@Setter
@ToString
public class AppendEntriesResponse extends RaftResponse {
  public static final RaftResponse.ResponseType SELF_TYPE =
          ResponseType.APPEND_ENTRIES;

  private long term;
  private boolean success;
  private int matchIndex;

  public AppendEntriesResponse(Peer from, long term, boolean success,
                               int matchIndex) {
    super(SELF_TYPE, from);
    this.term = term;
    this.success = success;
    this.matchIndex = matchIndex;
  }

  public int getMatchIndex() {
    return matchIndex;
  }

  @Override
  public long getTerm() {
    return term;
  }

  public boolean isSuccess() {
    return success;
  }
}
