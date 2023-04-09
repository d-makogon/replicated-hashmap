package ru.nsu.ccfit.dmakogon.network;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class AppendEntriesResponse extends RaftResponse {
  public static final RaftResponse.ResponseType SELF_TYPE =
          ResponseType.APPEND_ENTRIES;

  private long term;
  private boolean success;

  public AppendEntriesResponse(long term, boolean success) {
    super(SELF_TYPE);
    this.term = term;
    this.success = success;
  }
}
