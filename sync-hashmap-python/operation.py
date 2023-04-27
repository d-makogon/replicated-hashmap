from dataclasses import dataclass
import typing
from enum import Enum


class OperationType(Enum):
    INSERT = 1
    UPDATE = 2
    DELETE = 3


@dataclass
class Operation:
    term: int
    type: OperationType
    entry: typing.Tuple
