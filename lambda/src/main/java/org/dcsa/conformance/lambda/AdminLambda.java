package org.dcsa.conformance.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.*;
import lombok.extern.slf4j.Slf4j;
import org.dcsa.conformance.core.toolkit.JsonToolkit;
import org.dcsa.conformance.sandbox.ConformanceSandbox;

@Slf4j
public class AdminLambda implements RequestStreamHandler {

  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
    JsonNode jsonInput = JsonToolkit.inputStreamToJsonNode(inputStream);
    log.info("jsonInput = {}", jsonInput.toPrettyString());

    JsonNode jsonOutput =
        ConformanceSandbox.executeAdminTask(LambdaToolkit.createPersistenceProvider(), jsonInput);

    log.info("jsonOutput {}", jsonOutput.toPrettyString());
    JsonToolkit.writeJsonNodeToOutputStream(jsonOutput, outputStream);
  }

  public static void main(String[] args) {} // unused
}
