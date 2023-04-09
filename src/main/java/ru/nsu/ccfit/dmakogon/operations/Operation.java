package ru.nsu.ccfit.dmakogon.operations;

import ru.nsu.ccfit.dmakogon.storage.ReplicatedHashMapEntry;

public record Operation(long term, OperationType type,
                        ReplicatedHashMapEntry<?, ?> entry) {
}
