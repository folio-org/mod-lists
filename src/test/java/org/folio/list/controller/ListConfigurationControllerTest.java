package org.folio.list.controller;

import org.folio.list.context.TestcontainerCallbackExtension;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
// Using db-test profile here to prevent this test from waiting on the db connection timeout from application.yml
// This allows the test to run much faster, since it doesn't get stuck waiting for a db connection
@ActiveProfiles({"test", "db-test"})
@ExtendWith(TestcontainerCallbackExtension.class)
class ListConfigurationControllerTest {
  private static final String TENANT_ID = "test-tenant";

  @Autowired
  private MockMvc mockMvc;

  /**
   *
   * The purpose of this test is to verify that ListConfiguration is correctly read from the yaml file, and correctly
   * returned by the GET /lists/configuration API. Since the GET /lists/configuration API returns the ListConfiguration
   * directly, there is no way to mock the configuration itself.
   */
  @Test
  void shouldReturnListConfiguration() throws Exception {
    int expectedMaxListSize = 1250000;
    var requestBuilder = get("/lists/configuration")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);
    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.maxListSize", is(expectedMaxListSize)));
  }
}
