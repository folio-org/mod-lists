package org.folio.list.util;

import lombok.extern.log4j.Log4j2;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

@Log4j2
public class LogUtils {
  private LogUtils() {
    throw new UnsupportedOperationException("Trying to instantiate a utility class? Shame!");
  }

  public static String getSanitizedExceptionMessage(Throwable exception) {
    return Optional.ofNullable(exception)
      .map(LogUtils::getExceptionDetails)
      .map(LogUtils::sanitizeExceptionMessage)
      .orElse(null);
  }

  public static String sanitizeExceptionMessage(String message) {
    return Optional.ofNullable(message)
      .map(msg -> msg.replaceAll("(?i)Authorization: .*", "Authorization: [SANITIZED]")
        .replaceAll("(?i)x-amz-content-sha256: .*", "x-amz-content-sha256: [SANITIZED]")
        .replaceAll("(?i)x-amz-date: .*", "x-amz-date: [SANITIZED]")
        .replaceAll("(?i)x-amz-id-2: .*", "x-amz-id-2: [SANITIZED]")
        .replaceAll("(?i)x-amz-request-id: .*", "x-amz-request-id: [SANITIZED]")
        .replaceAll("(?i)requestId=.*", "requestId=[SANITIZED]")
        .replaceAll("(?i)hostId=.*", "hostId=[SANITIZED]")
        .replaceAll("Host: [^\\n]+", "Host: [SANITIZED]")
        .replaceAll("bucketName = [^,]+", "bucketName = [SANITIZED]")
        .replaceAll("objectName = [^,]+", "objectName = [SANITIZED]")
        .replaceAll("resource = [^,]+", "resource = [SANITIZED]")
        .replaceAll("(?i)(method=\\w+, url=)\\S+", "$1[SANITIZED]"))
      .orElse(null);
  }

  private static String getExceptionDetails(Throwable exception) {
    StringWriter stringWriter = new StringWriter();
    exception.printStackTrace(new PrintWriter(stringWriter));
    return stringWriter.toString();
  }
}
