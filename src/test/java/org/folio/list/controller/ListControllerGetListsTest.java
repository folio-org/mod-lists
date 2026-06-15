package org.folio.list.controller;

import org.folio.list.domain.dto.ListSummaryDTO;
import org.folio.list.domain.dto.ListSummaryResultsDTO;
import org.folio.list.services.ListService;
import org.folio.list.util.TestDataFixture;
import org.folio.spring.data.OffsetRequest;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
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

  @MockitoBean
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
     isNull(), isNull(), eq(false), isNull(), isNull())).thenReturn(listSummaryResultsDto);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(2)))
      .andExpect(jsonPath("$.totalPages", is(1)))
      .andExpect(jsonPath("$.content[0].name", is(listDto1.getName())))
      .andExpect(jsonPath("$.content[1].name", is(listDto2.getName())))
      .andExpect(jsonPath("$.content[0].entityTypeId", is(listDto1.getEntityTypeId().toString())))
      .andExpect(jsonPath("$.content[1].entityTypeId", is(listDto2.getEntityTypeId().toString())));

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(listService).getAllLists(pageableCaptor.capture(), isNull(), isNull(),
      isNull(), isNull(), eq(false), isNull(), isNull());
    assertSort(pageableCaptor.getValue(), "name", Sort.Direction.ASC);
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
      Mockito.eq(listEntityIds), Mockito.eq(true), Mockito.eq(true), Mockito.eq(false), Mockito.eq(providedTimestamp),
      isNull()))
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
    Pageable pageable = new OffsetRequest(
      offset,
      size,
      Sort.by(new Sort.Order(Sort.Direction.DESC, "successRefresh.recordsCount").nullsLast())
    );
    ListSummaryResultsDTO listSummaryResultsDto = getListSummaryResultsDTO(listDto1, listDto2);

    var requestBuilder = get("/lists")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .queryParam("offset", String.valueOf(offset))
      .queryParam("size", String.valueOf(size))
      .queryParam("search", "missing")
      .queryParam("sortBy", "recordsCount")
      .queryParam("sortOrder", "desc")
      .queryParam("active", "false")
      .queryParam("private", "false");

    when(listService.getAllLists(pageable, null,
      null, false, false, false, null, "missing")).thenReturn(listSummaryResultsDto);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(2)))
      .andExpect(jsonPath("$.totalPages", is(1)))
      .andExpect(jsonPath("$.content[0].name", is(listDto1.getName())))
      .andExpect(jsonPath("$.content[1].name", is(listDto2.getName())))
      .andExpect(jsonPath("$.content[0].entityTypeId", is(listDto1.getEntityTypeId().toString())))
      .andExpect(jsonPath("$.content[1].entityTypeId", is(listDto2.getEntityTypeId().toString())));

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(listService).getAllLists(pageableCaptor.capture(), isNull(),
      isNull(), eq(false), eq(false), eq(false), isNull(), eq("missing"));
    assertThat(pageableCaptor.getValue().getOffset()).isEqualTo(offset);
    assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(size);
    assertSort(pageableCaptor.getValue(), "successRefresh.recordsCount", Sort.Direction.DESC);
  }

  @ParameterizedTest
  @CsvSource({
    "name,asc,name,ASC",
    "name,desc,name,DESC",
    "updatedDate,asc,updatedDate,ASC",
    "updatedDate,desc,updatedDate,DESC",
    "recordsCount,asc,successRefresh.recordsCount,ASC",
    "recordsCount,desc,successRefresh.recordsCount,DESC"
  })
  void testGetAllListsWithSortableFields(String sortBy, String sortOrder, String expectedProperty,
                                         Sort.Direction expectedDirection) throws Exception {
    ListSummaryResultsDTO emptyResults = new ListSummaryResultsDTO()
      .content(List.of())
      .totalRecords(0L)
      .totalPages(0);

    var requestBuilder = get("/lists")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .queryParam("sortBy", sortBy)
      .queryParam("sortOrder", sortOrder);

    when(listService.getAllLists(any(Pageable.class), isNull(), isNull(),
      isNull(), isNull(), eq(false), isNull(), isNull())).thenReturn(emptyResults);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(0)))
      .andExpect(jsonPath("$.totalPages", is(0)));

    ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
    verify(listService).getAllLists(pageableCaptor.capture(), isNull(), isNull(),
      isNull(), isNull(), eq(false), isNull(), isNull());
    assertSort(pageableCaptor.getValue(), expectedProperty, expectedDirection);
  }

  @Test
  void testGetAllListsShouldRejectInvalidSortBy() throws Exception {
    var requestBuilder = get("/lists")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .queryParam("sortBy", "updated-date");

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("invalid.request")))
      .andExpect(jsonPath("$.parameters[0].key", is("error.reason")))
      .andExpect(jsonPath("$.parameters[0].value", containsString("getAllLists.sortBy")))
      .andExpect(jsonPath("$.parameters[0].value", containsString("name|updatedDate|recordsCount")));

    Mockito.verifyNoInteractions(listService);
  }

  @Test
  void testGetAllListsShouldRejectInvalidSortOrder() throws Exception {
    var requestBuilder = get("/lists")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .queryParam("sortOrder", "descending");

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("invalid.request")))
      .andExpect(jsonPath("$.parameters[0].key", is("error.reason")))
      .andExpect(jsonPath("$.parameters[0].value", containsString("getAllLists.sortOrder")))
      .andExpect(jsonPath("$.parameters[0].value", containsString("asc|desc")));

    Mockito.verifyNoInteractions(listService);
  }

  private ListSummaryResultsDTO getListSummaryResultsDTO(ListSummaryDTO listDto1, ListSummaryDTO listDto2) {
    List<ListSummaryDTO> summaryList = List.of(listDto1, listDto2);
    return new ListSummaryResultsDTO()
      .content(summaryList)
      .totalRecords(Long.valueOf(summaryList.size()))
      .totalPages(TOTAL_PAGES);
  }

  private void assertSort(Pageable pageable, String expectedProperty, Sort.Direction expectedDirection) {
    Sort.Order order = pageable.getSort().iterator().next();
    assertThat(order.getProperty()).isEqualTo(expectedProperty);
    assertThat(order.getDirection()).isEqualTo(expectedDirection);
    assertThat(order.getNullHandling()).isEqualTo(Sort.NullHandling.NULLS_LAST);
  }
}
