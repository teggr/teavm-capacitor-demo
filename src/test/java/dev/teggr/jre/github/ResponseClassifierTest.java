package dev.teggr.jre.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResponseClassifierTest {
    @Test
    void successHasNoError() {
        assertNull(ResponseClassifier.errorFor(200, "x", null, null));
    }

    @Test
    void notFoundNamesUser() {
        assertTrue(ResponseClassifier.errorFor(404, "ghost", null, null).contains("ghost"));
    }

    @Test
    void forbiddenWithExhaustedQuotaIsRateLimit() {
        assertTrue(ResponseClassifier.errorFor(403, "x", "0", null).contains("rate limit"));
    }

    @Test
    void tooManyRequestsIsRateLimitWithRetryHint() {
        String message = ResponseClassifier.errorFor(429, "x", null, "30");
        assertTrue(message.contains("rate limit") && message.contains("30"));
    }

    @Test
    void plainForbiddenIsNotRateLimit() {
        assertEquals("GitHub refused the request (HTTP 403).", ResponseClassifier.errorFor(403, "x", "55", null));
    }

    @Test
    void otherStatusesAreReported() {
        assertTrue(ResponseClassifier.errorFor(500, "x", null, null).contains("500"));
    }
}
