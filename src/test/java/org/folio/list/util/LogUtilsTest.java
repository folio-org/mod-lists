package org.folio.list.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class LogUtilsTest {
    @Test
    void shouldThrowErrorOnInstantiation() throws NoSuchMethodException {
        Constructor<LogUtils> constructor = LogUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Exception exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }

    @Test
    void shouldSanitizeExceptionMessage() {
        String inputMessage = """
                Authorization: SANITIZE_ME
                x-amz-content-sha256: SANITIZE_ME
                x-amz-date: SANITIZE_ME
                x-amz-id-2: SANITIZE_ME
                x-amz-request-id: SANITIZE_ME
                requestId=SANITIZE_ME
                hostId=SANITIZE_ME
                Host: some-host.sanitize-me.com
                method=POST, url=https://sanitize-me.com/api
                """;

        String expectedMessage = """
                Authorization: [SANITIZED]
                x-amz-content-sha256: [SANITIZED]
                x-amz-date: [SANITIZED]
                x-amz-id-2: [SANITIZED]
                x-amz-request-id: [SANITIZED]
                requestId=[SANITIZED]
                hostId=[SANITIZED]
                Host: [SANITIZED]
                method=POST, url=[SANITIZED]
                """;

        String actualMessage = LogUtils.sanitizeExceptionMessage(inputMessage);
        assertEquals(expectedMessage, actualMessage);
    }
}
