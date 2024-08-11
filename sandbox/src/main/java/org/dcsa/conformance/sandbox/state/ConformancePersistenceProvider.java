package org.dcsa.conformance.sandbox.state;

// PK=environment#UUID  SK=sandbox#UUID        value={id: sandboxUUID, name: sandboxName}
//
// PK=sandbox#UUID      SK=config              value={...}
// PK=sandbox#UUID      SK=state               value={currentSessionId: UUID, ...}  lock=...
// PK=sandbox#UUID      SK=waiting             value=[{"who": "Orchestrator", "forWhom": "Shipper1", "toDoWhat": "perform action 'UC1'"}, ...]
//
// PK=sandbox#UUID      SK=session#UTC#UUID
//
// PK=session#UUID      SK=state#orchestrator  value={...}                          lock=...
// PK=session#UUID      SK=state#party#NAME    value={...}                          lock=...
// PK=session#UUID      SK=exchange#UTC#UUID   value={...}

import lombok.Getter;
import org.dcsa.conformance.core.state.*;

@Getter
public class ConformancePersistenceProvider {
  private final SortedPartitionsNonLockingMap nonLockingMap;
  private final StatefulExecutor statefulExecutor;

  public ConformancePersistenceProvider(
      SortedPartitionsNonLockingMap nonLockingMap, SortedPartitionsLockingMap lockingMap) {
    this.nonLockingMap = nonLockingMap;
    this.statefulExecutor = new StatefulExecutor(lockingMap);
  }
}
