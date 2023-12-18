package org.folio.list.controller;

import static org.folio.spring.integration.XOkapiHeaders.USER_ID;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.folio.list.domain.dto.ListVersionDTO;
import org.folio.list.exception.ListNotFoundException;
import org.folio.list.services.ListService;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.integration.XOkapiHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ListController.class)
class ListControllerVersionTest {

  private static final String TENANT_ID = "test-tenant";

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ListService listService;

  @Test
  void testListVersion() throws Exception {
    UUID listId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ListVersionDTO listVersionDTO1 = TestDataFixture.getListVersionDTO();
    ListVersionDTO listVersionDTO2 = TestDataFixture.getListVersionDTO();
    List<ListVersionDTO> listVersionDTO = List.of(
      listVersionDTO1,
      listVersionDTO2
    );

    var requestBuilder = get("/lists/" + listId + "/versions")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(USER_ID, userId);

    when(listService.getListVersions(listId)).thenReturn(listVersionDTO);
    mockMvc
      .perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(
        jsonPath("$.[0].id", Matchers.is(listVersionDTO1.getId().toString()))
      )
      .andExpect(
        jsonPath(
          "$.[0].listId",
          Matchers.is(listVersionDTO1.getListId().toString())
        )
      )
      .andExpect(jsonPath("$.[0].name", Matchers.is(listVersionDTO1.getName())))
      .andExpect(
        jsonPath("$.[1].id", Matchers.is(listVersionDTO2.getId().toString()))
      )
      .andExpect(
        jsonPath(
          "$.[1].listId",
          Matchers.is(listVersionDTO2.getListId().toString())
        )
      )
      .andExpect(
        jsonPath("$.[1].name", Matchers.is(listVersionDTO2.getName()))
      );
  }

  @Test
  void shouldReturnHttp404WhenListNotFound() throws Exception {
    var requestBuilder = get("/lists/" + UUID.randomUUID() + "/versions")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListVersions(UUID.randomUUID()))
      .thenThrow(ListNotFoundException.class);

    mockMvc
      .perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("read-list.not.found")));
  }
}
