from dataclasses import dataclass
from typing import List


@dataclass
class PeerId:
    id: int

    def isValid(self):
        return self.id == -1


class PeerInfo:
    pass


class PeersList(List[PeerInfo]):
    pass
