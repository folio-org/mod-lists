package org.folio.list.controller;

import org.folio.list.domain.dto.RelatedUser;
import org.folio.list.domain.dto.RelatedUserCollection;
import org.folio.list.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RelatedUsersController.class)
class RelatedUsersControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @Test
  void getRelatedUsers_returnsOkWithCollection() throws Exception {
    String userId = UUID.randomUUID().toString();
    RelatedUser relatedUser = new RelatedUser().id(userId).fullName("Doe, John");
    RelatedUserCollection collection = new RelatedUserCollection()
      .relatedUsers(List.of(relatedUser))
      .totalRecords(1);

    when(userService.getRelatedUsersByRole("create")).thenReturn(collection);

    mockMvc.perform(get("/lists/related-users").param("role", "create"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(1)))
      .andExpect(jsonPath("$.relatedUsers", hasSize(1)))
      .andExpect(jsonPath("$.relatedUsers[0].id", is(userId)))
      .andExpect(jsonPath("$.relatedUsers[0].fullName", is("Doe, John")));
  }

  @Test
  void getRelatedUsers_emptyCollection_returnsOkWithZeroRecords() throws Exception {
    RelatedUserCollection emptyCollection = new RelatedUserCollection();

    when(userService.getRelatedUsersByRole("unknown")).thenReturn(emptyCollection);

    mockMvc.perform(get("/lists/related-users").param("role", "unknown"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.totalRecords", is(0)))
      .andExpect(jsonPath("$.relatedUsers", hasSize(0)));
  }
}
