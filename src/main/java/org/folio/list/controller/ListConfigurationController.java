package org.folio.list.controller;

import org.folio.list.domain.dto.ListConfiguration;
import org.folio.list.rest.resource.ListConfigurationApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class ListConfigurationController implements ListConfigurationApi {

  private final ListConfiguration listConfiguration;

  @Override
  public ResponseEntity<ListConfiguration> getListConfiguration() {
    return ResponseEntity.ok(listConfiguration);
  }
}
