package org.folio.list.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@FeignClient(name = "users")
public interface UsersClient {

  @GetMapping("/{userId}")
  User getUser(@RequestHeader UUID userId);

  @JsonIgnoreProperties(ignoreUnknown = true)
  record User(UUID id, Optional<Personal> personal) {
    public Optional<String> getFullName() {
      return personal.map(p -> String.format("%s, %s", p.lastName(), p.firstName()));
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true) // Ignore properties like 'middleName'
  record Personal(String firstName, String lastName) {}
}
