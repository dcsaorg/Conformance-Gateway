package org.dcsa.conformance.lambda;

import static org.dcsa.conformance.core.toolkit.JsonToolkit.OBJECT_MAPPER;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.*;
import java.nio.charset.StandardCharsets;
import lombok.extern.log4j.Log4j2;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.sandbox.ConformanceSandbox;
import org.dcsa.conformance.sandbox.state.ConformancePersistenceProvider;
import software.amazon.lambda.powertools.logging.Logging;

@Log4j2
public class SandboxTaskLambda implements RequestStreamHandler {

  @Logging
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
    try {
      JsonNode jsonInput = JsonToolkit.inputStreamToJsonNode(inputStream);
      log.info("jsonInput = {}", jsonInput);

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
      log.debug("jsonOutput = {}", jsonOutput);
      outputStream.write(jsonOutput.toString().getBytes(StandardCharsets.UTF_8));
    } catch (RuntimeException | Error e) {
      log.error("Unhandled exception: ", e);
      throw e;
    } catch (IOException e) {
      log.error("Unhandled exception: ", e);
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {} // unused
}
