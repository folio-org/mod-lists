package org.folio.list.controller;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.utils.DateMatcher;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class)
class ListControllerGetListByIdTest {
  private static final String TENANT_ID = "test-tenant";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ListService listService;

  @Test
  void testGetListByIdWithSuccessRefresh() throws Exception {
    UUID listId = UUID.randomUUID();
    var listDto = TestDataFixture.getListDTOSuccessRefresh(listId);

    var requestBuilder = get("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListById(listId)).thenReturn(Optional.of(listDto));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(listDto.getId().toString())))
      .andExpect(jsonPath("$.name", is(listDto.getName())))
      .andExpect(jsonPath("$.description", is(listDto.getDescription())))
      .andExpect(jsonPath("$.name", is(listDto.getName())))
      .andExpect(jsonPath("$.userFriendlyQuery", is(listDto.getUserFriendlyQuery())))
      .andExpect(jsonPath("$.fqlQuery", is(listDto.getFqlQuery())))
      .andExpect(jsonPath("$.createdByUsername", is(listDto.getCreatedByUsername())))
      .andExpect(jsonPath("$.createdDate", new DateMatcher(listDto.getCreatedDate())))
      .andExpect(jsonPath("$.isActive", is(listDto.getIsActive())))
      .andExpect(jsonPath("$.isPrivate", is(listDto.getIsPrivate())))
      .andExpect(jsonPath("$.isCanned", is(listDto.getIsCanned())))
      .andExpect(jsonPath("$.successRefresh.id", is(listDto.getSuccessRefresh().getId().toString())))
      .andExpect(jsonPath("$.successRefresh.listId", is(listDto.getSuccessRefresh().getListId().toString())))
      .andExpect(jsonPath("$.successRefresh.status", is(org.folio.list.domain.dto.ListRefreshDTO.StatusEnum.SUCCESS.getValue())))
      .andExpect(jsonPath("$.successRefresh.refreshStartDate", new DateMatcher(listDto.getSuccessRefresh().getRefreshStartDate())))
      .andExpect(jsonPath("$.successRefresh.refreshEndDate", new DateMatcher(listDto.getSuccessRefresh().getRefreshEndDate())))
      .andExpect(jsonPath("$.successRefresh.refreshedBy", is(listDto.getSuccessRefresh().getRefreshedBy().toString())))
      .andExpect(jsonPath("$.successRefresh.refreshedByUsername", is(listDto.getSuccessRefresh().getRefreshedByUsername())));
  }

  @Test
  void testGetListByIdWithInProgressRefresh() throws Exception {
    UUID listId = UUID.randomUUID();
    var listDto = TestDataFixture.getListDTOInProgressRefresh(listId);

    var requestBuilder = get("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListById(listId)).thenReturn(Optional.of(listDto));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(listDto.getId().toString())))
      .andExpect(jsonPath("$.name", is(listDto.getName())))
      .andExpect(jsonPath("$.description", is(listDto.getDescription())))
      .andExpect(jsonPath("$.name", is(listDto.getName())))
      .andExpect(jsonPath("$.userFriendlyQuery", is(listDto.getUserFriendlyQuery())))
      .andExpect(jsonPath("$.fqlQuery", is(listDto.getFqlQuery())))
      .andExpect(jsonPath("$.createdByUsername", is(listDto.getCreatedByUsername())))
      .andExpect(jsonPath("$.createdDate", new DateMatcher(listDto.getCreatedDate())))
      .andExpect(jsonPath("$.isActive", is(listDto.getIsActive())))
      .andExpect(jsonPath("$.isPrivate", is(listDto.getIsPrivate())))
      .andExpect(jsonPath("$.isCanned", is(listDto.getIsCanned())))
      .andExpect(jsonPath("$.inProgressRefresh.id", is(listDto.getInProgressRefresh().getId().toString())))
      .andExpect(jsonPath("$.inProgressRefresh.listId", is(listDto.getInProgressRefresh().getListId().toString())))
      .andExpect(jsonPath("$.inProgressRefresh.status", is(org.folio.list.domain.dto.ListRefreshDTO.StatusEnum.IN_PROGRESS.getValue())))
      .andExpect(jsonPath("$.inProgressRefresh.refreshedBy", is(listDto.getInProgressRefresh().getRefreshedBy().toString())))
      .andExpect(jsonPath("$.inProgressRefresh.refreshedByUsername", is(listDto.getInProgressRefresh().getRefreshedByUsername())));
  }

  @Test
  void testGetListByIdWithFailedRefresh() throws Exception {
    UUID listId = UUID.randomUUID();
    var listDto = TestDataFixture.getListDTOFailedRefresh(listId);

    var requestBuilder = get("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListById(listId)).thenReturn(Optional.of(listDto));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(listDto.getId().toString())))
      .andExpect(jsonPath("$.name", is(listDto.getName())))
      .andExpect(jsonPath("$.description", is(listDto.getDescription())))
      .andExpect(jsonPath("$.name", is(listDto.getName())))
      .andExpect(jsonPath("$.userFriendlyQuery", is(listDto.getUserFriendlyQuery())))
      .andExpect(jsonPath("$.fqlQuery", is(listDto.getFqlQuery())))
      .andExpect(jsonPath("$.createdByUsername", is(listDto.getCreatedByUsername())))
      .andExpect(jsonPath("$.createdDate", new DateMatcher(listDto.getCreatedDate())))
      .andExpect(jsonPath("$.isActive", is(listDto.getIsActive())))
      .andExpect(jsonPath("$.isPrivate", is(listDto.getIsPrivate())))
      .andExpect(jsonPath("$.isCanned", is(listDto.getIsCanned())))
      .andExpect(jsonPath("$.failedRefresh.id", is(listDto.getFailedRefresh().getId().toString())))
      .andExpect(jsonPath("$.failedRefresh.listId", is(listDto.getFailedRefresh().getListId().toString())))
      .andExpect(jsonPath("$.failedRefresh.status", is(org.folio.list.domain.dto.ListRefreshDTO.StatusEnum.FAILED.getValue())))
      .andExpect(jsonPath("$.failedRefresh.refreshedBy", is(listDto.getFailedRefresh().getRefreshedBy().toString())))
      .andExpect(jsonPath("$.failedRefresh.refreshedByUsername", is(listDto.getFailedRefresh().getRefreshedByUsername())));
  }


  @Test
  void shouldReturnHttp404WhenListNotFound() throws Exception {
    UUID listId = UUID.randomUUID();

    var requestBuilder = get("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListById(listId))
      .thenReturn(Optional.empty());

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("read-list.not.found")));
  }

  @Test
  void shouldReturnHttp401ForAccessingPrivateListId() throws Exception {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = new ListEntity();
    listEntity.setId(listId);

    var requestBuilder = get("/lists/" + listId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListById(listId))
      .thenThrow(new PrivateListOfAnotherUserException(listEntity, ListActions.READ));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.code", is("read-list.is.private")));
  }
}
