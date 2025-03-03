package org.folio.list.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.list.domain.dto.ListDTO;
import org.folio.list.domain.dto.ListRequestDTO;
import org.folio.list.exception.InsufficientEntityTypePermissionsException;
import org.folio.list.exception.InvalidFqlException;
import org.folio.list.services.ListActions;
import org.folio.list.services.ListService;
import org.folio.list.utils.DateMatcher;
import org.folio.list.utils.TestDataFixture;
import org.folio.spring.exception.NotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ListController.class)
class ListControllerCreateTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListService listService;

  @Test
  void testCreateList() throws Exception {
    UUID listId = UUID.randomUUID();
    ListDTO listDto = TestDataFixture.getListDTOSuccessRefresh(listId);
    ListRequestDTO listRequestDto = TestDataFixture.getListRequestDTO();
    ;

    var requestBuilder = post("/lists")
      .content(new ObjectMapper().writeValueAsString(listRequestDto))
      .contentType(APPLICATION_JSON);

    when(listService.createList(any(ListRequestDTO.class))).thenReturn(listDto);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isCreated())
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
      .andExpect(jsonPath("$.isCanned", is(listDto.getIsCanned())));
  }

  @Test
  void shouldReturnHttp400ForInvalidFql() throws Exception {
    ListRequestDTO listRequestDto = TestDataFixture.getListRequestDTO();
    ;
    when(listService.createList(listRequestDto))
      .thenThrow(new InvalidFqlException(listRequestDto.getFqlQuery(), ListActions.CREATE, Map.of("field1", "Field is invalid")));

    var requestBuilder = post("/lists")
      .content(new ObjectMapper().writeValueAsString(listRequestDto))
      .contentType(APPLICATION_JSON);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("create-fql.query.invalid")));
  }

  @Test
  void shouldReturnHttp403ForMissingEntityTypePermissions() throws Exception {
    ListRequestDTO listRequestDto = TestDataFixture.getListRequestDTO();
    String expectedErrorMessage = "User is missing permissions to access entity type "
      + listRequestDto.getEntityTypeId() + ". User is missing permissions: [foo.bar]";
    when(listService.createList(listRequestDto))
      .thenThrow(new InsufficientEntityTypePermissionsException(listRequestDto.getEntityTypeId(), ListActions.CREATE, "User is missing permissions: [foo.bar]"));

    var requestBuilder = post("/lists")
      .content(new ObjectMapper().writeValueAsString(listRequestDto))
      .contentType(APPLICATION_JSON);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isForbidden())
      .andExpect(jsonPath("$.code", is("create-entity.type.restricted")))
      .andExpect(jsonPath("$.message", is(expectedErrorMessage)));
  }

  @Test
  void shouldReturnHttp404WhenEntityTypeNotFound() throws Exception {
    ListRequestDTO listRequestDto = TestDataFixture.getListRequestDTO();
    String expectedErrorMessage = "Entity type with id " + listRequestDto.getEntityTypeId() + " was not found.";
    when(listService.createList(listRequestDto))
      .thenThrow(new NotFoundException("Entity type with id " + listRequestDto.getEntityTypeId() + " was not found."));

    var requestBuilder = post("/lists")
      .content(new ObjectMapper().writeValueAsString(listRequestDto))
      .contentType(APPLICATION_JSON);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("not.found")))
      .andExpect(jsonPath("$.message", is(expectedErrorMessage)));
  }

  @Test
  void shouldReturnErrorWhenRequiredFieldsMissing() throws Exception {
    ListRequestDTO requestWithMissingFields = TestDataFixture.getListRequestDTO();
    requestWithMissingFields.setEntityTypeId(null);
    requestWithMissingFields.setName("");

    var requestBuilder = post("/lists")
      .content(new ObjectMapper().writeValueAsString(requestWithMissingFields))
      .contentType(APPLICATION_JSON);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("invalid.request")))
      .andExpect(jsonPath("$.parameters[*].key", containsInAnyOrder("name", "entityTypeId")));
  }

  @Test
  void shouldReturnErrorWhenInvalidFieldsPresentInRequest() throws Exception {
    String requestWithUnknownField = """
        {
          "name": "Missing Items",
          "entityTypeId": "0cb79a4c-f7eb-4941-a104-745224ae0292",
          "isActive": true,
          "isPrivate": true,
          "unknownField": "some value"
        }
      """;
    var requestBuilder = post("/lists")
      .content(new ObjectMapper().writeValueAsString(requestWithUnknownField))
      .contentType(APPLICATION_JSON);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("invalid.request")));
  }
}
