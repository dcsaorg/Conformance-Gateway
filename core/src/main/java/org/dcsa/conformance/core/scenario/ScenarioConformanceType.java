package org.dcsa.conformance.core.scenario;

/**
 * How a scenario contributes to the conformance status of the module it belongs to.
 */
public enum ScenarioConformanceType {

  /**
   * The scenario must be executed conformantly.
   */
  REQUIRED,

  /**
   * The scenario is reported but never affects the conformance status.
   */
  OPTIONAL,

  /**
   * The scenario is one of a set of interchangeable alternatives within the same module: executing
   * any one of them conformantly is enough for the whole set to be conformant, and the alternatives
   * that were not executed are ignored.
   *
   * <p>All interchangeable scenarios in a module form a single group. Independent alternative
   * requirements must belong to separate modules; satisfying one module does not satisfy another.
   *
   * <p>Alternatives that <em>were</em> executed must still be conformant: not running a scenario is
   * allowed, but a failed or partially conformant alternative prevents the group from being fully
   * conformant, even when another alternative passed.
   */
  INTERCHANGEABLE;

  public boolean affectsOverallConformance() {
    return this != OPTIONAL;
  }

  public boolean isInterchangeable() {
    return this == INTERCHANGEABLE;
  }
}

