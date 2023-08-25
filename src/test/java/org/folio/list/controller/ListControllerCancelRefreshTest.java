package org.folio.list.controller;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.ListNotRefreshingException;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class)
class ListControllerCancelRefreshTest {

  private static final String TENANT_ID = "test-tenant";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ListService listService;

  @Test
  void shouldCancelListRefresh() throws Exception {
    UUID listId = UUID.randomUUID();

    var requestBuilder = delete("/lists/" + listId + "/refresh")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    doNothing().when(listService).cancelRefresh(listId);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNoContent());

    verify(listService, times(1)).cancelRefresh(listId);
  }

  @Test
  void cancelRefreshShouldThrowErrorWhenRefreshNotInProgress() throws Exception {
    ListEntity list = TestDataFixture.getListEntityWithSuccessRefresh();
    UUID listId = list.getId();

    var requestBuilder = delete("/lists/" + listId + "/refresh")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    doThrow(new ListNotRefreshingException(list, ListActions.CANCEL_REFRESH)).when(listService).cancelRefresh(listId);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("cancel_refresh-list.not.refreshing")));

    verify(listService, times(1)).cancelRefresh(listId);
  }
}
