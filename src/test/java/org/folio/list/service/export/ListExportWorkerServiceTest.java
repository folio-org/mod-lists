package org.folio.list.service.export;

import org.folio.list.domain.ExportDetails;
import org.folio.list.domain.ListEntity;
import org.folio.list.services.export.CsvCreator;
import org.folio.list.services.export.ExportLocalStorage;
import org.folio.list.services.export.ListExportWorkerService;
import org.folio.list.util.TestDataFixture;
import org.folio.querytool.domain.dto.EntityType;
import org.folio.s3.client.FolioS3Client;
import org.folio.spring.FolioExecutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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
    UUID userId = UUID.randomUUID();
    String tenantId = "tenant_01";

    String expectedDestinationFile = tenantId + "/" + exportId + ".csv";
    ExportLocalStorage localStorage = mock(ExportLocalStorage.class);
    ExportDetails exportDetails = getExportDetails(TestDataFixture.getPrivateListEntity(), exportId);
    String uploadId = "uploadId";
    ArrayList<String> partETags = new ArrayList<>();

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(csvCreator.createAndUploadCSV(exportDetails, expectedDestinationFile, uploadId, partETags, userId, null)).thenReturn(localStorage);
    when(folioS3Client.initiateMultipartUpload(expectedDestinationFile)).thenReturn(uploadId);
    boolean exportSucceeded = listExportWorkerService.doAsyncExport(exportDetails, userId, null).join();
    verify(folioS3Client, times(1)).completeMultipartUpload(expectedDestinationFile, uploadId, partETags);;
    assertTrue(exportSucceeded);
  }

  @Test
  void shouldReturnFailedFutureIfExportFail() {
    UUID exportId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    String tenantId = "tenant_01";
    String uploadId = "uploadId";
    ArrayList<String> partETags = new ArrayList<>();
    ExportLocalStorage localStorage = mock(ExportLocalStorage.class);
    String expectedDestinationFile = tenantId + "/" + exportId + ".csv";
    ExportDetails exportDetails = getExportDetails(TestDataFixture.getPrivateListEntity(), exportId);

    when(folioExecutionContext.getTenantId()).thenReturn(tenantId);
    when(folioExecutionContext.getUserId()).thenReturn(userId);
    when(folioS3Client.initiateMultipartUpload(expectedDestinationFile)).thenReturn(uploadId);
    when(csvCreator.createAndUploadCSV(exportDetails, expectedDestinationFile, uploadId, partETags, UUID.randomUUID(), null)).thenReturn(localStorage);
    doThrow(new RuntimeException("something went wrong")).when(folioS3Client).completeMultipartUpload(any(), any(), any());

    boolean exportFailed = listExportWorkerService.doAsyncExport(exportDetails, userId, null).isCompletedExceptionally();
    assertTrue(exportFailed);
  }

  private static ExportDetails getExportDetails(ListEntity entity, UUID exportId) {
    ExportDetails exportDetails = new ExportDetails();
    exportDetails.setExportId(exportId);
    exportDetails.setList(entity);
    return exportDetails;
  }
}
