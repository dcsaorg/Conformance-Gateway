package org.dcsa.conformance.core.check;

public record ConformanceError(String message, ConformanceErrorSeverity severity) {}
