from enum import Enum
from typing import Optional
from atomic import Atomic, AtomicInt
from operations_log import OperationsLog

from peer import PeerId, PeersList


class NodeType(Enum):
    LEADER = 1
    CANDIDATE = 2
    FOLLOWER = 3


class NodeState:
    NOT_VOTED_ID = PeerId(-1)

    type: NodeType
    currentTerm: AtomicInt
    votedFor: Atomic[PeerId]
    commitIndex: AtomicInt
    lastApplied: AtomicInt
    peersList: PeersList
    operationsLog: OperationsLog

    def getQuorum(self) -> int:
        return int((len(self.peersList) + 1) / 2 + 1)

    def getCurrentTerm(self) -> int:
        return self.currentTerm.get()

    def setCurrentTermToGreater(self, term: int) -> bool:
        return self.currentTerm.getAndUpdate(lambda x: max(x, term)) < term

    def incrementCurrentTerm(self) -> int:
        return self.currentTerm.incrementAndGet()

    def getVotedFor(self) -> Optional[PeerId]:
        value = self.votedFor.get()
        if value.isValid():
            return value
        return None

    def setVotedFor(self, votedFor: PeerId):
        self.votedFor.set(votedFor)

    def getCommitIndex(self):
        return self.commitIndex.get()

    def getLastApplied(self):
        return self.lastApplied.get()

    def incrementLastApplied(self):
        return self.lastApplied.incrementAndGet()

    def setCommitIndex(self, commitIndex: int):
        self.commitIndex.set(commitIndex)
