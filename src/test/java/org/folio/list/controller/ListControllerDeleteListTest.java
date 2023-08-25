package org.folio.list.controller;

import org.folio.list.domain.ListEntity;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.exception.ListIsCannedException;
import org.folio.list.exception.ListNotFoundException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.exception.RefreshInProgressException;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
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
class ListControllerDeleteListTest {

  private static final String TENANT_ID = "test-tenant";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ListService listService;

  @Test
  void testDeleteListById() throws Exception {
    UUID listId = UUID.randomUUID();

    var requestBuilder = delete("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    doNothing().when(listService).deleteList(listId);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNoContent());
  }

  @Test
  void shouldReturnHttp404WhenListNotFound() throws Exception {
    UUID listId = UUID.randomUUID();

    var requestBuilder = delete("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    doThrow(new ListNotFoundException(listId, ListActions.DELETE)).when(listService).deleteList(listId);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("delete-list.not.found")));
  }

  @Test
  void shouldReturnHttp401ForPrivateLists() throws Exception {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = new ListEntity();
    listEntity.setId(listId);

    var requestBuilder = delete("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    doThrow(new PrivateListOfAnotherUserException(listEntity, ListActions.DELETE)).when(listService).deleteList(listId);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.code", is("delete-list.is.private")));
  }

  @Test
  void shouldReturnHttp400ForCannedLists() throws Exception {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = new ListEntity();
    listEntity.setId(listId);

    var requestBuilder = delete("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    doThrow(new ListIsCannedException(listEntity, ListActions.DELETE)).when(listService).deleteList(listId);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("delete-list.is.canned")));
  }

  @Test
  void shouldReturnHttp400ForRefreshingList() throws Exception {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = new ListEntity();
    listEntity.setId(listId);
    ListRefreshDetails refreshDetails = new ListRefreshDetails();
    refreshDetails.setId(UUID.randomUUID());
    listEntity.setInProgressRefresh(refreshDetails);

    var requestBuilder = delete("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    doThrow(new RefreshInProgressException(listEntity, ListActions.DELETE)).when(listService).deleteList(listId);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("delete-list.refresh.in.progress")));
  }
}
