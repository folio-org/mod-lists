package org.folio.list.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.folio.list.domain.ListEntity;
import org.folio.list.exception.ExportInProgressException;
import org.folio.list.exception.ListNotFoundException;
import org.folio.list.services.ListActions;
import org.folio.list.services.export.ListExportService;
import org.folio.list.services.export.ListExportService.ExportDownloadContents;
import org.folio.spring.integration.XOkapiHeaders;
import org.folio.list.domain.dto.ListExportDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.folio.list.exception.ExportNotFoundException.exportNotFound;
import static org.folio.list.exception.ExportNotFoundException.inProgressExportNotFound;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

@WebMvcTest(ListExportController.class)
class ListExportControllerTest {

  private static final String TENANT_ID = "test-tenant";
  private static final String TEXT_CSV = "text/csv";

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private ListExportService listExportService;

  @Test
  void testListExport() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    List<String> fields = List.of("field1", "field2");
    ListExportDTO exportDTO = new ListExportDTO()
      .exportId(UUID.randomUUID())
      .listId(id)
      .status(ListExportDTO.StatusEnum.IN_PROGRESS)
      .createdBy(userId)
      .fields(fields);
    var requestBuilder = post("/lists/" + id + "/exports")
      .contentType(APPLICATION_JSON)
      .content(objectMapper.writeValueAsString(fields))
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, userId);

    when(listExportService.createExport(id, fields)).thenReturn(exportDTO);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.exportId", is(exportDTO.getExportId().toString())))
      .andExpect(jsonPath("$.listId", is(exportDTO.getListId().toString())))
      .andExpect(jsonPath("$.status", is(ListExportDTO.StatusEnum.IN_PROGRESS.getValue())))
      .andExpect(jsonPath("$.createdBy", is(exportDTO.getCreatedBy().toString())))
      .andExpect(jsonPath("$.fields", is(exportDTO.getFields())));
  }

  @Test
  void shouldThrow404WhenListForExportNotFound() throws Exception {
    UUID id = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    var requestBuilder = post("/lists/" + id + "/exports")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, userId);

    doThrow(new ListNotFoundException(id, ListActions.EXPORT)).when(listExportService).createExport(id, null);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("export-list.not.found")));
  }

  @Test
  void shouldReturnExportDetails() throws Exception {
    UUID exportId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();
    ListExportDTO exportDTO = new ListExportDTO()
      .exportId(exportId)
      .listId(listId)
      .status(ListExportDTO.StatusEnum.IN_PROGRESS)
      .createdBy(userId);
    var requestBuilder = get("/lists/" + listId + "/exports/" + exportId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, userId);

    when(listExportService.getExportDetails(listId, exportId)).thenReturn(exportDTO);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.exportId", is(exportDTO.getExportId().toString())))
      .andExpect(jsonPath("$.listId", is(exportDTO.getListId().toString())))
      .andExpect(jsonPath("$.status", is(ListExportDTO.StatusEnum.IN_PROGRESS.getValue())))
      .andExpect(jsonPath("$.createdBy", is(exportDTO.getCreatedBy().toString())));
  }

  @Test
  void shouldThrow404WhenExportIdNotFound() throws Exception {
    UUID exportId = UUID.randomUUID();
    UUID listId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    var requestBuilder = get("/lists/" + listId + "/exports/" + exportId)
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID)
      .header(XOkapiHeaders.USER_ID, userId);

    doThrow(exportNotFound(listId, exportId, ListActions.EXPORT)).when(listExportService).getExportDetails(listId, exportId);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("export-export.not.found")));
  }

  @Test
  void downloadExportTest() throws Exception {
    UUID listId = UUID.randomUUID();
    UUID exportId = UUID.randomUUID();

    String csvData = "xyz, item list, 25, patron";
    String listName = "Missing Items";
    ByteArrayInputStream csvInputStream = new ByteArrayInputStream(csvData.getBytes());

    var requestBuilder = get("/lists/" + listId + "/exports/" + exportId + "/download")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    when(listExportService.downloadExport(listId, exportId))
      .thenReturn(new ExportDownloadContents(listName, csvInputStream, 1234));

    var expectedContentDisposition = "attachment; filename=\"=?UTF-8?Q?%s?=\"; filename*=UTF-8''%s".formatted(
            listName.replace(' ', '_') + ".csv",
            URLEncoder.encode(listName, StandardCharsets.UTF_8).replace("+", "%20") + ".csv");
    mockMvc.perform(requestBuilder)
      .andExpect(status().isOk())
      .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, expectedContentDisposition))
      .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
      .andExpect(content().string(containsString(csvData)));
  }

  @Test
  void shouldThrowErrorWhenExportAlreadyInProgress() throws Exception {
    UUID listId = UUID.randomUUID();
    ListEntity listEntity = new ListEntity();
    listEntity.setId(listId);

    var requestBuilder = post("/lists/" + listId + "/exports")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    doThrow(new ExportInProgressException(listEntity, ListActions.EXPORT)).when(listExportService).createExport(listId, null);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.code", is("export-export.in.progress")));
  }

  @Test
  void shouldCancelExport() throws Exception {
    UUID listId = UUID.randomUUID();
    UUID exportId = UUID.randomUUID();
    var requestBuilder = post("/lists/" + listId + "/exports/" + exportId + "/cancel")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNoContent());

    verify(listExportService, times(1)).cancelExport(listId, exportId);
  }

  @Test
  void cancelExportShouldThrowErrorWhenExportNotInProgress() throws Exception {
    UUID listId = UUID.randomUUID();
    UUID exportId = UUID.randomUUID();
    var requestBuilder = post("/lists/" + listId + "/exports/" + exportId + "/cancel")
      .contentType(APPLICATION_JSON)
      .header(XOkapiHeaders.TENANT, TENANT_ID);

    doThrow(inProgressExportNotFound(listId, exportId, ListActions.CANCEL_EXPORT)).when(listExportService).cancelExport(listId, exportId);

    mockMvc.perform(requestBuilder)
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.code", is("cancel_export-export.not.in.progress")));

    verify(listExportService, times(1)).cancelExport(listId, exportId);
  }
}
