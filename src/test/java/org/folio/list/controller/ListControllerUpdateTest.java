package org.folio.list.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.list.domain.dto.ListUpdateRequestDTO;
import org.folio.list.exception.InvalidFqlException;
import org.folio.list.exception.OptimisticLockException;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.util.DateMatcher;
import org.folio.list.util.TestDataFixture;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class)
class ListControllerUpdateTest {
  private static final String TENANT_ID = "test-tenant";
  private static final String USER_ID = "413ce292-d38f-426f-bb10-2450f03b4705";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListService listService;

  @Test
  void testUpdateList() throws Exception {
    UUID listId = UUID.randomUUID();
    var listDto = TestDataFixture.getListDTOSuccessRefresh(listId);
    ListUpdateRequestDTO listUpdateRequestDto = TestDataFixture.getListUpdateRequestDTO();

    var requestBuilder = put("/lists/" + listId)
      .content(new ObjectMapper().writeValueAsString(listUpdateRequestDto))
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID);

    when(listService.updateList(any(UUID.class), any(ListUpdateRequestDTO.class))).thenReturn(Optional.of(listDto));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", is(listDto.getId().toString())))
      .andExpect(jsonPath("$.name", is(listDto.getName())))
      .andExpect(jsonPath("$.description", is(listDto.getDescription())))
      .andExpect(jsonPath("$.name", is(listDto.getName())))
      .andExpect(jsonPath("$.entityTypeId", is(listDto.getEntityTypeId().toString())))
      .andExpect(jsonPath("$.fqlQuery", is(listDto.getFqlQuery())))
      .andExpect(jsonPath("$.fields", is(listDto.getFields())))
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
  void shouldReturnHttp404WhenListNotFound() throws Exception {
    var listDto = TestDataFixture.getListDTOSuccessRefresh(UUID.randomUUID());
    ListUpdateRequestDTO listUpdateRequestDto = TestDataFixture.getListUpdateRequestDTO();;

    var requestBuilder = put("/lists/" + UUID.randomUUID())
      .content(new ObjectMapper().writeValueAsString(listUpdateRequestDto))
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID);

    when(listService.updateList(any(UUID.class), any(ListUpdateRequestDTO.class))).thenReturn(Optional.empty());

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("update-list.not.found")));
  }

  @Test
  void shouldReturnHttp400WhenVersionMismatch() throws Exception {
    UUID listId = UUID.randomUUID();
    ListUpdateRequestDTO listUpdateRequestDto = TestDataFixture.getListUpdateRequestDTO();
    var listEntity = TestDataFixture.getListEntityWithSuccessRefresh(listId);

    var requestBuilder = put("/lists/" + listId)
      .content(new ObjectMapper().writeValueAsString(listUpdateRequestDto))
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID);

    doThrow(new OptimisticLockException(listEntity, (int) Math.random()))
      .when(listService).updateList(listId, listUpdateRequestDto);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.code", is("update-optimistic.lock.exception")));
  }

  @Test
  void shouldReturnHttp400ForInvalidFql() throws Exception {
    UUID listId = UUID.randomUUID();
    ListUpdateRequestDTO updateRequest = TestDataFixture.getListUpdateRequestDTO();
    when(listService.updateList(listId, updateRequest))
      .thenThrow(new InvalidFqlException(updateRequest.getFqlQuery(), ListActions.UPDATE, Map.of("field1", "Field is invalid")));

    var requestBuilder = put("/lists/" + listId)
      .content(new ObjectMapper().writeValueAsString(updateRequest))
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, USER_ID);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("update-fql.query.invalid")));
  }
}
