from threading import Lock
from typing import Callable, Generic, TypeVar

T = TypeVar('T')


class Atomic(Generic[T]):
    def __init__(self, value: T):
        self.__value = value
        self.__lock = Lock()

    def get(self) -> T:
        with self.__lock:
            return self.__value

    def set(self, value: T) -> None:
        with self.__lock:
            self.__value = value

    def updateAndGet(self, func: Callable[[T], T]) -> T:
        with self.__lock:
            self.__value = func(self.__value)
            return self.__value

    def getAndUpdate(self, func: Callable[[T], T]) -> T:
        with self.__lock:
            old_value = self.__value
            self.__value = func(self.__value)
            return old_value


class AtomicInt:
    def __init__(self, value: int):
        self.__value = value
        self.__lock = Lock()

    def get(self) -> int:
        with self.__lock:
            return self.__value

    def set(self, value: int) -> None:
        with self.__lock:
            self.__value = value

    def updateAndGet(self, func: Callable[[int], int]) -> int:
        with self.__lock:
            self.__value = func(self.__value)
            return self.__value

    def getAndUpdate(self, func: Callable[[int], int]) -> int:
        with self.__lock:
            old_value = self.__value
            self.__value = func(self.__value)
            return old_value

    def incrementAndGet(self) -> int:
        return self.updateAndGet(lambda x: x + 1)


class AtomicBool:
    def __init__(self, value: bool):
        self.__value = value
        self.__lock = Lock()

    def get(self) -> bool:
        with self.__lock:
            return self.__value

    def set(self, value: bool) -> None:
        with self.__lock:
            self.__value = value
