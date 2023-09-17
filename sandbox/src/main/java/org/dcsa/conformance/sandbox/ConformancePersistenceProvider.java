package org.dcsa.conformance.sandbox;

// PK=environment#UUID      SK=sandbox#uuid      value={sandboxId: UUID}
//
// PK=sandbox#UUID          SK=config            value={...}
// PK=sandbox#UUID          SK=session#UTC#uuid  value={sessionId: UUID}
//
// PK=session#UUID          SK=state             value={...}              lock=...
// PK=session#UUID#traffic  SK=UTC#uuid          value={...}

import lombok.Getter;
import org.dcsa.conformance.core.state.SortedPartitionsLockingMap;
import org.dcsa.conformance.core.state.SortedPartitionsNonLockingMap;
import org.dcsa.conformance.core.state.StatefulExecutor;

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
