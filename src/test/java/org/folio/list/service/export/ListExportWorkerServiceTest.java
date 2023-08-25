package org.folio.list.service.export;

import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListEntity;
import org.folio.list.services.export.CsvCreator;
import org.folio.list.services.export.ExportLocalStorage;
import org.folio.list.services.export.ListExportWorkerService;
import org.folio.list.utils.TestDataFixture;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListExportWorkerServiceTest {
  @InjectMocks
  private ListExportWorkerService listExportWorkerService;
  @Mock
  private FolioExecutionContext folioExecutionContext;
  @Mock
  private FolioS3Client folioS3Client;
  @Mock
  private CsvCreator csvCreator;

  @Test
  void shouldExportList() {
    UUID exportId = UUID.randomUUID();
    String tenantId = "tenant_01";
    String localFile = "/path/to/file.csv";
    String expectedDestinationFile = tenantId + "/" + exportId + ".csv";
    ExportLocalStorage localStorage = mock(ExportLocalStorage.class);
    ExportDetails exportDetails = getExportDetails(TestDataFixture.getPrivateListEntity(), exportId);

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(localStorage.getAbsolutePath()).thenReturn(localFile);
    when(csvCreator.createCSV(exportDetails)).thenReturn(localStorage);

    boolean exportSucceeded = listExportWorkerService.doAsyncExport(exportDetails).join();
    verify(folioS3Client, times(1)).upload(localFile, expectedDestinationFile);
    assertTrue(exportSucceeded);
  }

  @Test
  void shouldReturnFailedFutureIfExportFail() {
    UUID exportId = UUID.randomUUID();
    String tenantId = "tenant_01";
    String localFile = "/path/to/file.csv";
    ExportLocalStorage localStorage = mock(ExportLocalStorage.class);
    ExportDetails exportDetails = getExportDetails(TestDataFixture.getPrivateListEntity(), exportId);

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(localStorage.getAbsolutePath()).thenReturn(localFile);
    when(csvCreator.createCSV(exportDetails)).thenReturn(localStorage);
    doThrow(new RuntimeException("something went wrong")).when(folioS3Client).upload(any(), any());

    boolean exportFailed = listExportWorkerService.doAsyncExport(exportDetails).isCompletedExceptionally();
    assertTrue(exportFailed);
  }

  private static ExportDetails getExportDetails(ListEntity entity, UUID exportId) {
    ExportDetails exportDetails = new ExportDetails();
    exportDetails.setExportId(exportId);
    exportDetails.setList(entity);
    return exportDetails;
  }
}
