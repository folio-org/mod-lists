package org.folio.list.controller;

import lombok.RequiredArgsConstructor;
import org.folio.list.domain.dto.ListExportDTO;
import org.folio.list.rest.resource.ListExportApi;
import org.folio.list.services.export.ListExportService;
import org.folio.list.services.export.ListExportService.ExportDownloadContents;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Lazy // Do not connect to S3 when the application starts
@RestController
@RequiredArgsConstructor
public class ListExportController implements ListExportApi {

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
    ExportDownloadContents download = listExportService.downloadExport(id, exportId);
    String fileName = download.listName() + ".csv";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentDisposition(ContentDisposition.builder("attachment").filename(fileName, StandardCharsets.UTF_8).build());
    return ResponseEntity.ok()
      .headers(headers)
      .contentType(MediaType.APPLICATION_OCTET_STREAM)
      .body(new InputStreamResource(download.stream()));
  }

  @Override
  public ResponseEntity<Void> cancelExport(UUID listId, UUID exportId) {
    listExportService.cancelExport(listId, exportId);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
