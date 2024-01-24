package org.folio.list.controller;

import org.folio.list.domain.dto.ListSummaryDTO;
import org.folio.list.domain.dto.ListSummaryResultsDTO;
import org.folio.list.services.ListService;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class)
class ListControllerGetListsTest {

  private static final String TENANT_ID = "test-tenant";
  public static final int TOTAL_PAGES = 1;

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ListService listService;

  @Test
  void testGetAllLists() throws Exception {
    UUID listId1 = UUID.randomUUID();
    var listDto1 = TestDataFixture.getListSummaryDTO(listId1);

    UUID listId2 = UUID.randomUUID();
    var listDto2 = TestDataFixture.getListSummaryDTO(listId2);

    ListSummaryResultsDTO listSummaryResultsDto = getListSummaryResultsDTO(listDto1, listDto2);

    var requestBuilder = get("/lists")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getAllLists(any(Pageable.class), isNull(), isNull(),
     isNull(), isNull(), isNull(), isNull())).thenReturn(listSummaryResultsDto);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(2)))
      .andExpect(jsonPath("$.totalPages", is(1)))
      .andExpect(jsonPath("$.content[0].name", is(listDto1.getName())))
      .andExpect(jsonPath("$.content[1].name", is(listDto2.getName())))
      .andExpect(jsonPath("$.content[0].entityTypeId", is(listDto1.getEntityTypeId().toString())))
      .andExpect(jsonPath("$.content[1].entityTypeId", is(listDto2.getEntityTypeId().toString())));
  }

  @Test
  void testGetAllListsWithIdsParameter() throws Exception {
    UUID listId1 = UUID.randomUUID();
    org.folio.list.domain.dto.ListSummaryDTO listDto1 = TestDataFixture.getListSummaryDTO(listId1);
    UUID listId2 = UUID.randomUUID();
    org.folio.list.domain.dto.ListSummaryDTO listDto2 = TestDataFixture.getListSummaryDTO(listId2);
    List<UUID> listIds = List.of(listId1, listId2);
    List<UUID> listEntityIds = List.of(listDto1.getEntityTypeId(), listDto2.getEntityTypeId());
    String updatedAsOf = "2023-01-27T20:54:41.528281 05:30";
    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    OffsetDateTime providedTimestamp = OffsetDateTime.parse(updatedAsOf.replace(' ', '+'), formatter);

    ListSummaryResultsDTO listSummaryResultsDto = getListSummaryResultsDTO(listDto1, listDto2);
    RequestBuilder requestBuilder = get("/lists")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .queryParam("ids", listId1.toString(), listId2.toString())
      .queryParam("entityTypeIds", listDto1.getEntityTypeId().toString(), listDto2.getEntityTypeId().toString())
      .queryParam("active", "true")
      .queryParam("private", "true")
      .queryParam("includeDeleted", "false")
      .queryParam("updatedAsOf", "2023-01-27T20:54:41.528281+05:30");


    when(listService.getAllLists(any(Pageable.class), Mockito.eq(listIds),
      Mockito.eq(listEntityIds), Mockito.eq(true), Mockito.eq(true), Mockito.eq(false), Mockito.eq(providedTimestamp)))
      .thenReturn(listSummaryResultsDto);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(2)))
      .andExpect(jsonPath("$.totalPages", is(1)))
      .andExpect(jsonPath("$.content[0].name", is(listDto1.getName())))
      .andExpect(jsonPath("$.content[1].name", is(listDto2.getName())))
      .andExpect(jsonPath("$.content[0].entityTypeId", is(listDto1.getEntityTypeId().toString())))
      .andExpect(jsonPath("$.content[1].entityTypeId", is(listDto2.getEntityTypeId().toString())));
  }

  @Test
  void testGetAllListsWithVariableOffsetAndSize() throws Exception {
    UUID listId1 = UUID.randomUUID();
    var listDto1 = TestDataFixture.getListSummaryDTO(listId1);

    UUID listId2 = UUID.randomUUID();
    var listDto2 = TestDataFixture.getListSummaryDTO(listId2);

    int offset = 1;
    int size = 20;
    Pageable pageable = new OffsetRequest(offset, size);
    List<org.folio.list.domain.dto.ListSummaryDTO> lists = List.of(listDto1, listDto2);
    ListSummaryResultsDTO listSummaryResultsDto = getListSummaryResultsDTO(listDto1, listDto2);

    var requestBuilder = get("/lists")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .queryParam("offset", String.valueOf(offset))
      .queryParam("size", String.valueOf(size))
      .queryParam("active", "false")
      .queryParam("private", "false");

    when(listService.getAllLists(pageable, null,
      null, false, false, false, null)).thenReturn(listSummaryResultsDto);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(2)))
      .andExpect(jsonPath("$.totalPages", is(1)))
      .andExpect(jsonPath("$.content[0].name", is(listDto1.getName())))
      .andExpect(jsonPath("$.content[1].name", is(listDto2.getName())))
      .andExpect(jsonPath("$.content[0].entityTypeId", is(listDto1.getEntityTypeId().toString())))
      .andExpect(jsonPath("$.content[1].entityTypeId", is(listDto2.getEntityTypeId().toString())));
  }

  private ListSummaryResultsDTO getListSummaryResultsDTO(ListSummaryDTO listDto1, ListSummaryDTO listDto2) {
    List<ListSummaryDTO> summaryList = List.of(listDto1, listDto2);
    return new ListSummaryResultsDTO()
      .content(summaryList)
      .totalRecords(Long.valueOf(summaryList.size()))
      .totalPages(TOTAL_PAGES);
  }
}
