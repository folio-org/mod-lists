package org.folio.list.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

@HttpExchange(url = "users")
public interface UsersClient {
  @GetExchange("/{userId}")
  User getUser(@PathVariable UUID userId);

  @JsonIgnoreProperties(ignoreUnknown = true)
  record User(UUID id, Optional<Personal> personal) {
    public Optional<String> getFullName() {
      return personal.map(p -> String.format("%s, %s", p.lastName(), p.firstName()));
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true) // Ignore properties like 'middleName'
  record Personal(String firstName, String lastName) {}
}
