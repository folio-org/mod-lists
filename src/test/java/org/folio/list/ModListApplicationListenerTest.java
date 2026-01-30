package org.folio.list;

import org.folio.s3.client.FolioS3Client;
import org.folio.s3.exception.S3ClientException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class ModListApplicationListenerTest extends ModListApplicationTest {
  @Test
  void failedS3ChecksShouldPreventStartup() {
    FolioS3Client folioS3Client = mock(FolioS3Client.class);
    Mockito.when(folioS3Client.write(anyString(), any(InputStream.class))).thenThrow(new RuntimeException());

    ListsApplicationListener listsApplicationListener = new ListsApplicationListener(folioS3Client);
    ReflectionTestUtils.setField(listsApplicationListener, "s3CheckEnabled", true);

    assertThrows(RuntimeException.class, () -> listsApplicationListener.onApplicationEvent(null));
  }

  @Test
  void failedBucketInitializationShouldPreventStartup() {
    FolioS3Client folioS3Client = mock(FolioS3Client.class);
    Mockito.doThrow(new RuntimeException()).when(folioS3Client).createBucketIfNotExists();

    ListsApplicationListener listsApplicationListener = new ListsApplicationListener(folioS3Client);
    ReflectionTestUtils.setField(listsApplicationListener, "s3CheckEnabled", true);

    assertThrows(S3ClientException.class, () -> listsApplicationListener.onApplicationEvent(null));
    Mockito.verify(folioS3Client, times(1)).createBucketIfNotExists();
  }

  @Test
  void bucketInitializationShouldBeSkippedWhenStartupCheckIsDisabled() {
    FolioS3Client folioS3Client = mock(FolioS3Client.class);

    ListsApplicationListener listsApplicationListener = new ListsApplicationListener(folioS3Client);
    ReflectionTestUtils.setField(listsApplicationListener, "s3CheckEnabled", false);

    assertDoesNotThrow(() -> listsApplicationListener.onApplicationEvent(null));
    Mockito.verify(folioS3Client, never()).createBucketIfNotExists();
  }
}
