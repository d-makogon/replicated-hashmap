from threading import Lock
from typing import List, Optional
from operation import Operation


class OperationsLog:
    EMPTY_LOG_LAST_INDEX = -1

    def __init__(self) -> None:
        self.log: List[Operation] = []
        self.appendLock = Lock()

    def append(self, operation: Operation) -> None:
        with self.appendLock:
            self.log.append(operation)

    def tryGet(self, index: int) -> Optional[Operation]:
        try:
            return self.log[index]
        except IndexError:
            return None

    def getTerm(self, index: int) -> int:
        if (index > self.EMPTY_LOG_LAST_INDEX):
            return self.log[index].term
        return 0

    def getLastIndex(self) -> int:
        return len(self.log) - 1

    def getLastTerm(self) -> int:
        return self.getTerm(self.getLastIndex())

    def removeAllFromIndex(self, index: int) -> None:
        with self.appendLock:
            del self.log[index:]

    def allFromIndex(self, index: int) -> List[Operation]:
        return self.log[index:]
