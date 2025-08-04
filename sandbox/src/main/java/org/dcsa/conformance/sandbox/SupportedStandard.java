package org.dcsa.conformance.sandbox;

import java.util.Arrays;
import org.dcsa.conformance.core.AbstractStandard;
import org.dcsa.conformance.standards.adoption.AdoptionStandard;
import org.dcsa.conformance.standards.an.AnStandard;
import org.dcsa.conformance.standards.booking.BookingStandard;
import org.dcsa.conformance.standards.cs.CsStandard;
import org.dcsa.conformance.standards.ebl.EblStandard;
import org.dcsa.conformance.standards.eblinterop.PintStandard;
import org.dcsa.conformance.standards.eblissuance.EblIssuanceStandard;
import org.dcsa.conformance.standards.eblsurrender.EblSurrenderStandard;
import org.dcsa.conformance.standards.jit.JitStandard;
import org.dcsa.conformance.standards.ovs.OvsStandard;
import org.dcsa.conformance.standards.tnt.TntStandard;

public enum SupportedStandard {
  ADOPTION(AdoptionStandard.INSTANCE),
  ARRIVAL_NOTICE(AnStandard.INSTANCE),
  BOOKING(BookingStandard.INSTANCE),
  CS(CsStandard.INSTANCE),
  EBL(EblStandard.INSTANCE),
  EBL_ISSUANCE(EblIssuanceStandard.INSTANCE),
  EBL_SURRENDER(EblSurrenderStandard.INSTANCE),
  JIT(JitStandard.INSTANCE),
  OVS(OvsStandard.INSTANCE),
  PINT(PintStandard.INSTANCE),
  TNT(TntStandard.INSTANCE);

  public final AbstractStandard standard;

  SupportedStandard(AbstractStandard standard) {
    this.standard = standard;
  }

  public static SupportedStandard forName(String standardName) {
    return Arrays.stream(values())
        .filter(supportedStandard -> supportedStandard.standard.getName().equals(standardName))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Unsupported standard name %s".formatted(standardName)));
  }
}
