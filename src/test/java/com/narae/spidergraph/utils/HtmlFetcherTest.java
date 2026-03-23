package com.narae.spidergraph.utils;

import org.jsoup.nodes.Document;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class HtmlFetcherTest {

    @Test
    public void connectorFactoryReturnsNewInstances() {
        HtmlFetcher.Connector first = HtmlFetcher.connector();
        HtmlFetcher.Connector second = HtmlFetcher.connector();

        assertNotSame(first, second);
    }

    @Test
    public void userAgentReturnsSameConnectorAndStoresValue() throws Exception {
        HtmlFetcher.Connector connector = HtmlFetcher.connector();

        HtmlFetcher.Connector result = connector.userAgent("UnitTestAgent/1.0");

        assertSame(connector, result);
        assertEquals("UnitTestAgent/1.0", readField(connector, "userAgent"));
    }

    @Test
    public void timeoutReturnsSameConnectorAndStoresValue() throws Exception {
        HtmlFetcher.Connector connector = HtmlFetcher.connector();

        HtmlFetcher.Connector result = connector.timeout(4321);

        assertSame(connector, result);
        assertEquals(4321, readField(connector, "timeout"));
    }

    @Test
    public void fetchReturnsNullWhenUrlIsEmpty() {
        Document document = HtmlFetcher.connector()
                .userAgent("Mozilla")
                .timeout(10000)
                .fetch("");

        assertNull(document);
    }

    @Test
    public void fetchReturnsNullWhenUrlIsNull() {
        Document document = HtmlFetcher.connector()
                .userAgent("Mozilla")
                .timeout(10000)
                .fetch(null);

        assertNull(document);
    }

    @Test
    public void fetchThrowsForMalformedNonAbsoluteUrl() {
        HtmlFetcher.Connector connector = HtmlFetcher.connector();

        assertThrows(IllegalArgumentException.class, () -> connector.fetch("not-a-valid-url"));
    }

    private static Object readField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
