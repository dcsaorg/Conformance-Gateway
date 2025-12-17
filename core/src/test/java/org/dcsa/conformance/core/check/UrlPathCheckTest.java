package org.dcsa.conformance.core.check;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import java.util.UUID;
import org.dcsa.conformance.core.traffic.ConformanceExchange;
import org.dcsa.conformance.core.traffic.ConformanceRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UrlPathCheckTest {

    private final UUID dummyUuid = UUID.randomUUID();

    @Test
    void testCheckConformance_MatchSingleExpectedSuffix() {
        String url = "https://api.example.com/bookings/123";
        UrlPathCheck check = new UrlPathCheck(role -> true, dummyUuid, "/bookings/123");

        ConformanceExchange exchange = mockExchangeWithUrl(url);

        ConformanceCheckResult.SimpleErrors result = (ConformanceCheckResult.SimpleErrors) check.performCheck(uuid -> dummyUuid.equals(uuid) ? exchange : null);

        assertEquals(Set.of(), result.errors());
    }

    @Test
    void testCheckConformance_MatchMultipleSuffixes() {
        String url = "https://api.example.com/bookings/123/cancel";
        UrlPathCheck check = new UrlPathCheck(role -> true, dummyUuid, "/cancel", "/submit");

        ConformanceExchange exchange = mockExchangeWithUrl(url);

        ConformanceCheckResult.SimpleErrors result = (ConformanceCheckResult.SimpleErrors) check.performCheck(uuid -> dummyUuid.equals(uuid) ? exchange : null);

        assertEquals(Set.of(), result.errors());
    }

    @Test
    void testCheckConformance_NoMatchOnAnySuffix() {
        String url = "https://api.example.com/bookings/123/status";
        UrlPathCheck check = new UrlPathCheck(role -> true, dummyUuid, "/cancel", "/submit");

        ConformanceExchange exchange = mockExchangeWithUrl(url);

        ConformanceCheckResult.SimpleErrors result = (ConformanceCheckResult.SimpleErrors) check.performCheck(uuid -> dummyUuid.equals(uuid) ? exchange : null);

        assertEquals(1, result.errors().size());

        String errorMessage = result.errors().iterator().next();
        assertTrue(errorMessage.contains(url), "Error should contain the URL");
        assertTrue(errorMessage.contains("/cancel"), "Error should mention /cancel");
        assertTrue(errorMessage.contains("/submit"), "Error should mention /submit");
        assertTrue(errorMessage.startsWith("Request URL"), "Error should start with 'Request URL'");
    }

    @Test
    void testCheckConformance_ExchangeIsNull() {
        UrlPathCheck check = new UrlPathCheck(role -> true, dummyUuid, "/bookings");

        ConformanceCheckResult.SimpleErrors result = (ConformanceCheckResult.SimpleErrors) check.performCheck(uuid -> null);

        assertEquals(Set.of(), result.errors());
    }

    private ConformanceExchange mockExchangeWithUrl(String url) {
        ConformanceExchange exchange = Mockito.mock(ConformanceExchange.class);
        ConformanceRequest request = Mockito.mock(ConformanceRequest.class);
        Mockito.when(exchange.getRequest()).thenReturn(request);
        Mockito.when(request.url()).thenReturn(url);
        return exchange;
    }
}
