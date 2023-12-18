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
import org.folio.list.services.ListActions;
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

  private static final UUID TEST_LIST_ID = UUID.fromString("ca08e0e4-ceef-5456-821f-b44309f0f77e");
  private static final UUID TEST_USER_ID = UUID.fromString("f3ed39d2-b0da-571a-9917-0e6d31e144aa");

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ListService listService;

  @Test
  void testListVersion() throws Exception {
    ListVersionDTO listVersionDTO1 = TestDataFixture.getListVersionDTO();
    ListVersionDTO listVersionDTO2 = TestDataFixture.getListVersionDTO();
    List<ListVersionDTO> listVersionDTO = List.of(
      listVersionDTO1,
      listVersionDTO2
    );

    var requestBuilder = get("/lists/" + TEST_LIST_ID + "/versions")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(USER_ID, TEST_USER_ID);

    when(listService.getListVersions(TEST_LIST_ID)).thenReturn(listVersionDTO);
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
  void testListsVersionsNotFound() throws Exception {
    var requestBuilder = get("/lists/" + TEST_LIST_ID + "/versions")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListVersions(TEST_LIST_ID))
      .thenThrow(new ListNotFoundException(TEST_LIST_ID, ListActions.READ));

    mockMvc
      .perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("read-list.not.found")));
  }
}
