package com.narae.spidergraph.crawler;

import com.narae.spidergraph.model.CrawlerSettings;
import com.narae.spidergraph.model.PageGraph;
import com.narae.spidergraph.model.PageNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SearchTest {

    @After
    public void tearDown() throws Exception {
        Search.resetDocumentFetcher();
        Search.resetSleeper();
        Search.resetClock();
        Search.resetRequestThrottle();
        resetSearchState();
    }

    @Test
    public void verifyUrl() throws Exception {
        PageGraph graph = Search.sync("https://fly.io/docs/about/pricing/", 0, settings(1, true, null));

        assertNotNull(graph);
    }

    @Test
    public void syncBuildsGraphFromMockedHtmlFetcher() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        Document rootDocument = html("https://example.com/root", "Root", "<a href='/child'>Child</a>");
        Document childDocument = html("https://example.com/child", "Child", "<p>Child body</p>");
        documents.put("https://example.com/root", rootDocument);
        documents.put("https://example.com/child", childDocument);

        Search.setDocumentFetcher((url, settings) -> documents.get(url));

        PageGraph graph = Search.sync("https://example.com/root", 0, settings(1, true, null));

        PageNode rootNode = graph.getNodes().get("https://example.com/root");
        PageNode childNode = graph.getNodes().get("https://example.com/child");

        assertEquals(2, graph.getNodes().size());
        assertTrue(rootNode.getOutgoing().contains(childNode));
        assertTrue(childNode.getIncoming().contains(rootNode));
        assertNotNull(childNode.getText());
        assertTrue(childNode.getText().contains("Child body"));
    }

    @Test
    public void syncVisitsAllValidLinksOnTheSamePage() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        Document rootDocument = html(
                "https://example.com/root",
                "Root",
                "<a href='/first'>First</a><a href='/second'>Second</a>"
        );
        Document firstDocument = html("https://example.com/first", "First", "<p>First body</p>");
        Document secondDocument = html("https://example.com/second", "Second", "<p>Second body</p>");
        documents.put("https://example.com/root", rootDocument);
        documents.put("https://example.com/first", firstDocument);
        documents.put("https://example.com/second", secondDocument);

        Search.setDocumentFetcher((url, settings) -> documents.get(url));

        PageGraph graph = Search.sync("https://example.com/root", 0, settings(1, true, null));

        PageNode rootNode = graph.getNodes().get("https://example.com/root");
        PageNode firstNode = graph.getNodes().get("https://example.com/first");
        PageNode secondNode = graph.getNodes().get("https://example.com/second");

        assertEquals(3, graph.getNodes().size());
        assertTrue(rootNode.getOutgoing().contains(firstNode));
        assertTrue(rootNode.getOutgoing().contains(secondNode));
        assertTrue(firstNode.getIncoming().contains(rootNode));
        assertTrue(secondNode.getIncoming().contains(rootNode));
    }

    @Test
    public void asyncSkipsLinksOutsideConfiguredPrefix() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        Document rootDocument = html(
                "https://example.com/root",
                "Root",
                "<a href='https://example.com/allowed/page'>Allowed</a>" +
                        "<a href='https://example.com/rejected'>Rejected</a>"
        );
        Document allowedDocument = html("https://example.com/allowed/page", "Allowed", "<p>Allowed body</p>");
        documents.put("https://example.com/root", rootDocument);
        documents.put("https://example.com/allowed/page", allowedDocument);

        Search.setDocumentFetcher((url, settings) -> documents.get(url));

        PageGraph graph = Search.async(
                "https://example.com/root",
                0,
                settings(1, false, "https://example.com/allowed")
        ).join();

        assertEquals(2, graph.getNodes().size());
        assertTrue(graph.getNodes().containsKey("https://example.com/root"));
        assertTrue(graph.getNodes().containsKey("https://example.com/allowed/page"));
        assertFalse(graph.getNodes().containsKey("https://example.com/rejected"));
    }

    @Test
    public void asyncAppliesSharedThrottleAcrossConcurrentTasks() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        documents.put(
                "https://example.com/root",
                html(
                        "https://example.com/root",
                        "Root",
                        "<a href='/first'>First</a><a href='/second'>Second</a>"
                )
        );
        documents.put("https://example.com/first", html("https://example.com/first", "First", "<p>One</p>"));
        documents.put("https://example.com/second", html("https://example.com/second", "Second", "<p>Two</p>"));

        AtomicLong now = new AtomicLong(1000L);
        List<Long> fetchTimes = new CopyOnWriteArrayList<>();

        Search.setClock(now::get);
        Search.setSleeper(now::addAndGet);
        Search.setDocumentFetcher((url, settings) -> {
            fetchTimes.add(now.get());
            return documents.get(url);
        });

        CrawlerSettings settings = settings(1, true, null);
        settings.setRequestDelay(250);

        PageGraph graph = Search.async("https://example.com/root", 0, settings).join();

        List<Long> orderedTimes = new ArrayList<>(fetchTimes);
        orderedTimes.sort(Long::compareTo);

        assertEquals(3, graph.getNodes().size());
        assertEquals(3, orderedTimes.size());
        assertEquals(1000L, orderedTimes.get(0).longValue());
        assertEquals(1250L, orderedTimes.get(1).longValue());
        assertEquals(1500L, orderedTimes.get(2).longValue());
    }

    @Test
    public void syncAppliesSharedThrottleBetweenSequentialRequests() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        documents.put(
                "https://example.com/root",
                html("https://example.com/root", "Root", "<a href='/child'>Child</a>")
        );
        documents.put("https://example.com/child", html("https://example.com/child", "Child", "<p>Body</p>"));

        AtomicLong now = new AtomicLong(2000L);
        List<Long> fetchTimes = new ArrayList<>();

        Search.setClock(now::get);
        Search.setSleeper(now::addAndGet);
        Search.setDocumentFetcher((url, settings) -> {
            fetchTimes.add(now.get());
            return documents.get(url);
        });

        CrawlerSettings settings = settings(1, true, null);
        settings.setRequestDelay(300);

        PageGraph graph = Search.sync("https://example.com/root", 0, settings);

        assertEquals(2, graph.getNodes().size());
        assertEquals(List.of(2000L, 2300L), fetchTimes);
    }

    @Test
    public void syncStopsGracefullyWhenHookReturnsFalse() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        documents.put(
                "https://example.com/root",
                html(
                        "https://example.com/root",
                        "Root",
                        "<a href='/first'>First</a><a href='/second'>Second</a>"
                )
        );
        documents.put("https://example.com/first", html("https://example.com/first", "First", "<p>One</p>"));
        documents.put("https://example.com/second", html("https://example.com/second", "Second", "<p>Two</p>"));

        AtomicInteger hookCalls = new AtomicInteger();
        Search.setDocumentFetcher((url, settings) -> documents.get(url));

        CrawlerSettings settings = settings(1, true, null);
        settings.setCrawlStepHook(context -> hookCalls.incrementAndGet() < 2);

        PageGraph graph = Search.sync("https://example.com/root", 0, settings);

        assertEquals(2, hookCalls.get());
        assertEquals(Set.of("https://example.com/root", "https://example.com/first"), graph.getNodes().keySet());
        assertFalse(graph.getNodes().containsKey("https://example.com/second"));
    }

    @Test
    public void asyncStopsGracefullyWhenHookReturnsFalse() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        documents.put("https://example.com/root", html("https://example.com/root", "Root", "<a href='/child'>Child</a>"));
        documents.put("https://example.com/child", html("https://example.com/child", "Child", "<a href='/grandchild'>Grandchild</a>"));
        documents.put("https://example.com/grandchild", html("https://example.com/grandchild", "Grandchild", "<p>Done</p>"));

        AtomicInteger hookCalls = new AtomicInteger();
        Search.setDocumentFetcher((url, settings) -> documents.get(url));

        CrawlerSettings settings = settings(2, true, null);
        settings.setCrawlStepHook(context -> hookCalls.incrementAndGet() < 2);

        PageGraph graph = Search.async("https://example.com/root", 0, settings).join();

        assertEquals(2, hookCalls.get());
        assertEquals(Set.of("https://example.com/root", "https://example.com/child"), graph.getNodes().keySet());
        assertFalse(graph.getNodes().containsKey("https://example.com/grandchild"));
    }

    private static CrawlerSettings settings(int maxDepth, boolean verifyRootHost, String urlPrefix) {
        CrawlerSettings settings = new CrawlerSettings();
        settings.setMaxDepth(maxDepth);
        settings.setVerifyRootHost(verifyRootHost);
        settings.setRootHost("example.com");
        settings.setTimeout(1000);
        settings.setUserAgent("SearchTest/1.0");
        settings.setUrlPrefix(urlPrefix);
        return settings;
    }

    private static Document html(String url, String title, String body) {
        return Jsoup.parse("<html><head><title>" + title + "</title></head><body>" + body + "</body></html>", url);
    }

    private static void resetSearchState() throws Exception {
        Field pageGraphField = Search.class.getDeclaredField("pageGraph");
        pageGraphField.setAccessible(true);
        PageGraph graph = (PageGraph) pageGraphField.get(null);
        graph.getNodes().clear();

        Field visitedField = Search.class.getDeclaredField("visited");
        visitedField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> visited = (Set<String>) visitedField.get(null);
        visited.clear();
    }
}
