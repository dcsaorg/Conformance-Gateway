package org.dcsa.conformance.core.scenario;

import java.util.Objects;

/**
 * Reference that points to the value of a previous reference until a current value is set.
 * To be used in conformance scenarios for storing values that are "learned" during later actions
 * without affecting the later validations of earlier actions.
 *
 * @param <V> the referenced value type
 */
public class OverwritingReference<V> {
  private final OverwritingReference<V> previousReference;
  private V currentValue;

  public OverwritingReference(OverwritingReference<V> previousReference, V value) {
    this.currentValue = value;
    this.previousReference = value != null ? previousReference : Objects.requireNonNull(previousReference);
  }

  public boolean hasCurrentValue() {
    return currentValue != null;
  }

  public V get() {
    return currentValue != null ? currentValue : previousReference.get();
  }

  public void set(V value) {
    if (previousReference == null && value == null) {
      throw new IllegalArgumentException("Cannot empty a reference that has no previous reference to default to.");
    }
    this.currentValue = value;
  }

  @Override
  public boolean equals(Object o) {
    throw new IllegalArgumentException("OverwritingReference should not be compared for equality.");
  }

  @Override
  public int hashCode() {
    throw new IllegalArgumentException("OverwritingReference should not be hashed.");
  }
}
