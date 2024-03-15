package org.folio.list.controller;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.folio.list.domain.dto.ListExportDTO;
import org.folio.list.rest.resource.ListExportApi;
import org.folio.list.services.export.ListExportService;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@Lazy // Do not connect to S3 when the application starts
public class ListExportController implements ListExportApi {

  private static final String TEXT_CSV = "text/csv";

  private final ListExportService listExportService;

  @Override
  public ResponseEntity<ListExportDTO> exportList(UUID listId, List<String> fields) {
    var listExportDto = listExportService.createExport(listId, fields);
    return new ResponseEntity<>(listExportDto, HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<ListExportDTO> getExportDetails(UUID listId, UUID exportId) {
    var listExportDto = listExportService.getExportDetails(listId, exportId);
    return new ResponseEntity<>(listExportDto, HttpStatus.OK);
  }

  @Override
  public ResponseEntity<Resource> downloadList(UUID id, UUID exportId) {
    Pair<String, InputStream> listNameAndCsvPair = listExportService.downloadExport(id, exportId);
    var resource = new InputStreamResource(listNameAndCsvPair.getRight());
    String fileName = listNameAndCsvPair.getLeft() + ".csv";
    var headers = new HttpHeaders();
    headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName, StandardCharsets.UTF_8).build());
    return ResponseEntity.ok()
      .headers(headers)
      .contentType(MediaType.valueOf(TEXT_CSV))
      .body(resource);
  }

  @Override
  public ResponseEntity<Void> cancelExport(UUID listId, UUID exportId) {
    listExportService.cancelExport(listId, exportId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
