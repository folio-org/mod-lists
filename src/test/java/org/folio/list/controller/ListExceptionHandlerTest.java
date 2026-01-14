package org.folio.list.controller;

import org.folio.list.services.ListService;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class) // This is an arbitrary controller, just to get something to test with
class ListExceptionHandlerTest {

  private static final String TENANT_ID = "test-tenant";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListService listService;

  @Test
  void handleGenericExceptions() throws Exception {
    // Given a listId and a broken listService that always throws an unhandled exception
    var listId = UUID.randomUUID();
    doThrow(new RuntimeException()).when(listService).deleteList(listId);

    // When we do a request that will cause an unhandled exception to get thrown
    mockMvc.perform(delete("/lists/" + listId).header(XOkapiHeaders.TENANT, TENANT_ID))
      // Then we expect a 500 Internal Server Error response with a mod-lists error code
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.code", is("unhandled.error")));

    // Sanity check to ensure that the unhandled exception came from the expectedg place
    verify(listService, times(1)).deleteList(listId);
  }
}
