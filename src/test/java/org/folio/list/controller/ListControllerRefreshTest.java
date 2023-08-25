package org.folio.list.controller;

import org.folio.list.domain.ListEntity;
import org.folio.list.domain.ListRefreshDetails;
import org.folio.list.exception.ListInactiveException;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.exception.RefreshInProgressException;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class)
class ListControllerRefreshTest {

  private static final String TENANT_ID = "test-tenant";
  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ListService listService;

  @Test
  void shouldPerformRefresh() throws Exception {
    UUID listId = UUID.randomUUID();
    org.folio.list.domain.dto.ListRefreshDTO listRefreshDTO = TestDataFixture.getListRefreshDTO();
    listRefreshDTO.setStatus(org.folio.list.domain.dto.ListRefreshDTO.StatusEnum.IN_PROGRESS);

    var requestBuilder = post("/lists/" + listId + "/refresh")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.performRefresh(listId)).thenReturn(Optional.of(listRefreshDTO));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(listRefreshDTO.getId().toString())))
      .andExpect(jsonPath("$.listId", is(listRefreshDTO.getListId().toString())))
      .andExpect(jsonPath("$.status", is(listRefreshDTO.getStatus().toString())))
      .andExpect(jsonPath("$.refreshedBy", is(listRefreshDTO.getRefreshedBy().toString())))
      .andExpect(jsonPath("$.refreshedByUsername", is(listRefreshDTO.getRefreshedByUsername())));
  }

  @Test
  void refreshShouldReturnHttp404WhenListNotFound() throws Exception {
    UUID listId = UUID.randomUUID();

    var requestBuilder = post("/lists/" + listId + "/refresh")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, listId);

    when(listService.performRefresh(listId))
      .thenReturn(Optional.empty());

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("refresh-list.not.found")));
  }

  @Test
  void refreshShouldReturnHttpError400WhenListInactive() throws Exception {
    String tenantId = "tenant_02";
    UUID listId = UUID.randomUUID();
    ListEntity list = new ListEntity();
    list.setId(listId);

    var requestBuilder = post("/lists/" + listId + "/refresh")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, tenantId);

    when(listService.performRefresh(listId))
      .thenThrow(new ListInactiveException(list, ListActions.REFRESH));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("refresh-list.is.inactive")));
  }

  @Test
  void refreshShouldReturnHttpError400WhenRefreshAlreadyInProgress() throws Exception {
    String tenantId = "tenant_02";
    UUID listId = UUID.randomUUID();
    ListEntity list = new ListEntity();
    list.setId(listId);
    ListRefreshDetails refreshDetails = new ListRefreshDetails();
    refreshDetails.setId(UUID.randomUUID());
    list.setInProgressRefresh(refreshDetails);

    var requestBuilder = post("/lists/" + listId + "/refresh")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, tenantId);

    when(listService.performRefresh(listId))
      .thenThrow(new RefreshInProgressException(list, ListActions.REFRESH));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("refresh-list.refresh.in.progress")));
  }

  @Test
  void shouldReturnHttp401ForRefreshingPrivateList() throws Exception {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = new ListEntity();
    listEntity.setId(listId);
    String tenantId = "tenant_02";

    var requestBuilder = post("/lists/" + listId + "/refresh")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, tenantId);

    when(listService.performRefresh(listId))
      .thenThrow(new PrivateListOfAnotherUserException(listEntity, ListActions.REFRESH));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.code", is("refresh-list.is.private")));
  }
}
