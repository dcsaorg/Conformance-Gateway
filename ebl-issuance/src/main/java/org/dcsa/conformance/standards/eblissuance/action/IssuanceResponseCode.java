package org.dcsa.conformance.standards.eblissuance.action;

public enum IssuanceResponseCode {
    ACCEPTED("ISSU"),
    BLOCKED("BDOC"),
    REFUSED("REFU");
    public final String standardCode;
    IssuanceResponseCode(String standardCode) {
        this.standardCode = standardCode;
    }
}
