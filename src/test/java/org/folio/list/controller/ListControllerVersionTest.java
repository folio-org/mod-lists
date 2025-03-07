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
import org.folio.list.exception.VersionNotFoundException;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.util.TestDataFixture;
import org.folio.spring.integration.XOkapiHeaders;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ListController.class)
class ListControllerVersionTest {

  private static final String TENANT_ID = "test-tenant";

  private static final UUID TEST_LIST_ID = UUID.fromString(
    "ca08e0e4-ceef-5456-821f-b44309f0f77e"
  );
  private static final UUID TEST_USER_ID = UUID.fromString(
    "f3ed39d2-b0da-571a-9917-0e6d31e144aa"
  );
  private static final int TEST_VERSION_NUMBER = 7;

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListService listService;

  @Test
  void testGetAllListVersions() throws Exception {
    ListVersionDTO listVersionDTO1 = TestDataFixture.getListVersionDTO();
    ListVersionDTO listVersionDTO2 = TestDataFixture.getListVersionDTO();
    List<ListVersionDTO> listVersionDTO = List.of(
      listVersionDTO1,
      listVersionDTO2
    );

    MockHttpServletRequestBuilder requestBuilder = get(
      "/lists/" + TEST_LIST_ID + "/versions"
    )
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
  void testGetAllListsVersionsErrorListNotFound() throws Exception {
    MockHttpServletRequestBuilder requestBuilder = get(
      "/lists/" + TEST_LIST_ID + "/versions"
    )
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListVersions(TEST_LIST_ID))
      .thenThrow(new ListNotFoundException(TEST_LIST_ID, ListActions.READ));

    mockMvc
      .perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("read-list.not.found")));
  }

  @Test
  void testGetSingleListVersion() throws Exception {
    ListVersionDTO expected = TestDataFixture.getListVersionDTO();

    MockHttpServletRequestBuilder requestBuilder = get(
      "/lists/" + TEST_LIST_ID + "/versions/" + TEST_VERSION_NUMBER
    )
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(USER_ID, TEST_USER_ID);

    when(listService.getListVersion(TEST_LIST_ID, TEST_VERSION_NUMBER))
      .thenReturn(expected);
    mockMvc
      .perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id", Matchers.is(expected.getId().toString())))
      .andExpect(
        jsonPath("$.listId", Matchers.is(expected.getListId().toString()))
      )
      .andExpect(jsonPath("$.name", Matchers.is(expected.getName())));
  }

  @Test
  void testGetSingleListVersionErrorListNotFound() throws Exception {
    MockHttpServletRequestBuilder requestBuilder = get(
      "/lists/" + TEST_LIST_ID + "/versions/" + TEST_VERSION_NUMBER
    )
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListVersion(TEST_LIST_ID, TEST_VERSION_NUMBER))
      .thenThrow(new ListNotFoundException(TEST_LIST_ID, ListActions.READ));

    mockMvc
      .perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("read-list.not.found")));
  }

  @Test
  void testGetSingleListVersionErrorVersionNotFound() throws Exception {
    MockHttpServletRequestBuilder requestBuilder = get(
      "/lists/" + TEST_LIST_ID + "/versions/" + TEST_VERSION_NUMBER
    )
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listService.getListVersion(TEST_LIST_ID, TEST_VERSION_NUMBER))
      .thenThrow(
        new VersionNotFoundException(
          TEST_LIST_ID,
          TEST_VERSION_NUMBER,
          ListActions.READ
        )
      );

    mockMvc
      .perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("read-version.not.found")));
  }
}
