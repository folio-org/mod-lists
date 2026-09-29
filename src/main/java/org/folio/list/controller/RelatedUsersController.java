package org.folio.list.controller;

import lombok.RequiredArgsConstructor;
import org.folio.list.domain.dto.RelatedUserCollection;
import org.folio.list.rest.resource.ListRelatedUsersApi;
import org.folio.list.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RelatedUsersController implements ListRelatedUsersApi {
  private final UserService userService;

  @Override
  public ResponseEntity<RelatedUserCollection> getRelatedUsers(String role) {
    return new ResponseEntity<>(userService.getRelatedUsersByRole(role), HttpStatus.OK);
  }
}
