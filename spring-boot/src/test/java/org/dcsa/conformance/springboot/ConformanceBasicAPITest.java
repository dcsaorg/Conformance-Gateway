package org.dcsa.conformance.springboot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Basic validation on API endpoints: testing content type, status code, and basic content.
 * Does not run the actual conformance tests, because Tomcat is not started, so no actual endpoints are reachable by a httpClient.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ConformanceBasicAPITest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ConformanceApplication app;

  private static final String SANDBOX_ID = "booking-200-conformance-auto-all-in-one";

  @Test
  void shouldReturnDefaultHomepage() throws Exception {
    mockMvc.perform(get("/"))
//      .andDo(print())
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
      .andExpect(content().string(containsString("DCSA Conformance")))
      .andExpect(content().string(containsString(SANDBOX_ID)))
      .andExpect(content().string(containsString("tnt-220-conformance-auto-all-in-one")));
  }

  @Test
  void shouldStartBookingScenario() throws Exception {
    mockMvc.perform(get(getAppURL(SANDBOX_ID, "reset")))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(content().string(containsString("{}")));
  }

  @Test
  void shouldReturnStatus() throws Exception {
    mockMvc.perform(get(getAppURL(SANDBOX_ID, "reset"))).andExpect(status().isOk());

    mockMvc.perform(get(getAppURL(SANDBOX_ID, "status")))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(content().string(containsString("{\"scenariosLeft\":14}")));
  }

  @Test
  void shouldReturnReport() throws Exception {
    mockMvc.perform(get(getAppURL(SANDBOX_ID, "reset"))).andExpect(status().isOk());

    mockMvc.perform(get(getAppURL(SANDBOX_ID, "report")))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
      .andExpect(content().string(containsString("Conformance Report")))
      .andExpect(content().string(containsString("Carrier conformance")))
      .andExpect(content().string(containsString("Shipper conformance")));
  }

  @Test
  void shouldReturnPrintableReport() throws Exception {
    mockMvc.perform(get(getAppURL(SANDBOX_ID, "reset"))).andExpect(status().isOk());

    mockMvc.perform(get(getAppURL(SANDBOX_ID, "printableReport")))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
      .andExpect(content().string(containsString("Conformance Report")))
      .andExpect(content().string(containsString("Carrier conformance")))
      .andExpect(content().string(containsString("Shipper conformance")));

    mockMvc.perform(get(getAppURL(SANDBOX_ID, "party/Carrier1/prompt/json")))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(content().string(containsString("SupplyCSP [REGULAR]")));
  }

  @Test
  void shouldReturnCarrier1Prompt() throws Exception {
    mockMvc.perform(get(getAppURL(SANDBOX_ID, "reset"))).andExpect(status().isOk());

    mockMvc.perform(get(getAppURL(SANDBOX_ID, "party/Carrier1/prompt/json")))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(content().string(containsString("SupplyCSP [REGULAR]")));
  }

  @Test
  void shouldReturnPlatform1Prompt() throws Exception {
    mockMvc.perform(get(getAppURL(SANDBOX_ID, "reset"))).andExpect(status().isOk());

    mockMvc.perform(get(getAppURL(SANDBOX_ID, "party/Platform1/prompt/json")))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(content().string(containsString("[ ]")));
  }

  @Test
  void shouldReturnWebUIHandlerResponse() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders
        .post("/conformance/webui/")
        .content("{\"operation\":\"getAllSandboxes\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON))
      .andExpect(status().isOk())
      .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
      .andExpect(content().string(containsString("auto-all-in-one")));
  }

  private String getAppURL(String scenarioID, String urlPath) {
    return "/conformance/" + app.localhostAuthUrlToken + "/sandbox/" + scenarioID + "/" + urlPath;
  }

}
