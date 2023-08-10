package org.dcsa.conformance.gateway.standards.eblsurrender.v10.scenarios;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;

public class EblSurrenderV10AsyncRequestAction extends EblSurrenderV10Action {
  private String surrenderRequestReference = null;

  @Getter
  private final Consumer<String> srrConsumer =
      srr -> {
        if (surrenderRequestReference != null) {
          throw new IllegalStateException();
        }
        this.surrenderRequestReference = srr;
      };

  @Getter
  private final Supplier<String> srrSupplier =
      () -> {
        if (surrenderRequestReference == null) {
          throw new IllegalStateException();
        }
        return surrenderRequestReference;
      };

  public EblSurrenderV10AsyncRequestAction(
      Supplier<String> tdrSupplier, String sourcePartyName, String targetPartyName) {
    super(tdrSupplier, sourcePartyName, targetPartyName);
  }

  public ObjectNode asJsonNode() {
    return super.asJsonNode().put("srr", srrSupplier.get());
  }
}
