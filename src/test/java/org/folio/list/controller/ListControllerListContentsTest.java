package org.folio.list.controller;

import org.folio.list.domain.ListEntity;
import org.folio.list.exception.PrivateListOfAnotherUserException;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.querytool.domain.dto.ResultsetPage;
import org.folio.spring.integration.XOkapiHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class)
class ListControllerListContentsTest {
  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListService listService;

  @Test
  void shouldGetListContents() throws Exception {
    UUID listId = UUID.randomUUID();
    Integer offset = 0;
    Integer size = 2;
    List<String> fields = List.of("key1", "key2", "key3", "key4");
    var requestBuilder = get("/lists/" + listId + "/contents?size=2&offset=0&fields=key1,key2,key3,key4")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, listId);
    List<Map<String, Object>> expectedList = List.of(
      Map.of("key1", "value1", "key2", "value2"),
      Map.of("key3", "value3", "key4", "value4"));
    Optional<ResultsetPage> expectedContent = Optional.of(new ResultsetPage().content(expectedList).totalRecords(expectedList.size()));
    when(listService.getListContents(listId, fields, offset, size)).thenReturn(expectedContent);
    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.content[0]", is(expectedList.get(0))))
      .andExpect(jsonPath("$.content[1]", is(expectedList.get(1))));
  }

  @Test
  void getListContentsShouldReturnHttp404WhenListNotFound() throws Exception {
    UUID listId = UUID.randomUUID();
    Integer offset = 0;
    Integer size = 0;
    List<String> fields = List.of("key1");
    var requestBuilder = get("/lists/" + listId + "/contents?size=0&offset=0&fields=key1")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, listId);
    Optional<ResultsetPage> expectedContent = Optional.empty();
    when(listService.getListContents(listId, fields, offset, size)).thenReturn(expectedContent);
    mockMvc.perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("read-list.not.found")));
  }

  @Test
  void shouldReturnHttp401ForAccessingPrivateListContents() throws Exception {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = new ListEntity();
    listEntity.setId(listId);
    Integer offset = 0;
    Integer size = 0;
    List<String> fields = List.of("key1");

    var requestBuilder = get("/lists/" + listId + "/contents?size=0&offset=0&fields=key1")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, listId);

    when(listService.getListContents(listId, fields, offset, size))
      .thenThrow(new PrivateListOfAnotherUserException(listEntity, ListActions.READ));

    mockMvc.perform(requestBuilder)
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$.code", is("read-list.is.private")));
  }
}
