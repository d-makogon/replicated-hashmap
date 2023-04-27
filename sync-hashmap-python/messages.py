from dataclasses import dataclass
from typing import List
from operation import Operation
from peer import PeerId
from enum import Enum


class MessageType(Enum):
    APPEND_ENTRIES = 1
    REQUEST_VOTE = 2


@dataclass
class RaftMessage:
    fromPeer: PeerId
    term: int
    type: MessageType


@dataclass
class RaftRequest(RaftMessage):
    pass


@dataclass
class RaftResponse(RaftMessage):
    pass


class AppendEntriesRequest(RaftRequest):
    leaderId: PeerId
    prevLogIndex: int
    prevLogTerm: int
    entries: List[Operation]
    leaderCommit: int

    def __init__(self,
                 fromPeer: PeerId,
                 term: int,
                 leaderId: PeerId,
                 prevLogIndex: int,
                 prevLogTerm: int,
                 entries: List[Operation],
                 leaderCommit: int):
        super().__init__(fromPeer, term, MessageType.APPEND_ENTRIES)
        self.leaderId = leaderId
        self.prevLogIndex = prevLogIndex
        self.prevLogTerm = prevLogTerm
        self.entries = entries
        self.leaderCommit = leaderCommit

    def isHeartbeat(self) -> bool:
        return len(self.entries) == 0


class AppendEntriesResponse(RaftResponse):
    success: bool
    matchIndex: int

    def __init__(self,
                 fromPeer: PeerId,
                 term: int,
                 success: bool,
                 matchIndex: int):
        super().__init__(fromPeer, term, MessageType.APPEND_ENTRIES)
        self.success = success
        self.matchIndex = matchIndex


class RequestVoteRequest(RaftRequest):
    candidateId: PeerId
    lastLogIndex: int
    lastLogTerm: int

    def __init__(self,
                 fromPeer: PeerId,
                 term: int,
                 candidateId: PeerId,
                 lastLogIndex: int,
                 lastLogTerm: int):
        super().__init__(fromPeer, term, MessageType.REQUEST_VOTE)
        self.candidateId = candidateId
        self.lastLogIndex = lastLogIndex
        self.lastLogTerm = lastLogTerm


class RequestVoteResponse(RaftResponse):
    voteGranted: bool

    def __init__(self,
                 fromPeer: PeerId,
                 term: int,
                 voteGranted: bool):
        super().__init__(fromPeer, term, MessageType.REQUEST_VOTE)
        self.voteGranted = voteGranted
