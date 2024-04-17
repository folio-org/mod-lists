package org.folio.list;

import org.folio.s3.client.FolioS3Client;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

class ModListApplicationListenerTest extends ModListApplicationTest {
  @Test
  void failedS3ChecksShouldPreventStartup() {
    FolioS3Client folioS3Client = mock(FolioS3Client.class);
    Mockito.when(folioS3Client.write(anyString(), any(InputStream.class))).thenThrow(new RuntimeException());

    ListsApplicationListener listsApplicationListener = new ListsApplicationListener(folioS3Client);
    ReflectionTestUtils.setField(listsApplicationListener, "s3CheckEnabled", true);

    assertThrows(RuntimeException.class, () -> listsApplicationListener.onApplicationEvent(null));
  }
}
