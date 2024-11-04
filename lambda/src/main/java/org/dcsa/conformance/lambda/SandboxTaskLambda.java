package org.dcsa.conformance.lambda;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;


@Slf4j
public class SandboxTaskLambda implements RequestStreamHandler {

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
    try {
      JsonNode jsonInput = JsonToolkit.inputStreamToJsonNode(inputStream);
      log.info("jsonInput = {}", jsonInput.toPrettyString());

      try {
        Thread.sleep(100); // for consistency with the local app
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      ConformancePersistenceProvider persistenceProvider =
          LambdaToolkit.createPersistenceProvider();

      ConformanceSandbox.executeDeferredTask(
          persistenceProvider,
          LambdaToolkit.createDeferredSandboxTaskConsumer(persistenceProvider),
          jsonInput);

      ObjectNode jsonOutput = OBJECT_MAPPER.createObjectNode();
      log.info("jsonOutput {}", jsonOutput.toPrettyString());
      try (BufferedWriter writer =
          new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
        writer.write(jsonOutput.toPrettyString());
        writer.flush();
      }
    } catch (RuntimeException | Error e) {
      log.error("Unhandled exception: {}", e, e);
      throw e;
    } catch (IOException e) {
      log.error("Unhandled exception: {}", e, e);
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {} // unused
}
