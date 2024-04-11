package org.dcsa.conformance.standards.ebl.models;

@FunctionalInterface
public interface TriConsumer<T, U, V> {
  void accept(T t, U u, V v);
}
