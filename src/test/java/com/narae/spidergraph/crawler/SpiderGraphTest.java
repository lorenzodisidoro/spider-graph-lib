package com.narae.spidergraph.crawler;

import com.narae.spidergraph.model.PageGraph;
import com.narae.spidergraph.model.PageNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SpiderGraphTest {

    @After
    public void tearDown() throws Exception {
        Search.resetDocumentFetcher();
        Search.resetSleeper();
        Search.resetClock();
        Search.resetRequestThrottle();
        resetSearchState();
        resetCrawlerStatics();
    }

    @Test
    public void crawlerFactoryReturnsDistinctInstances() {
        SpiderGraph.Crawler first = SpiderGraph.crawler();
        SpiderGraph.Crawler second = SpiderGraph.crawler();

        assertNotSame(first, second);
    }

    @Test
    public void startSynchronousSearchUsesConfiguredFetcherSettings() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        Document rootDocument = html("https://example.com/root", "Root", "<a href='/allowed'>Allowed</a>");
        Document childDocument = html("https://example.com/allowed", "Allowed", "<p>Child page</p>");
        documents.put("https://example.com/root", rootDocument);
        documents.put("https://example.com/allowed", childDocument);

        String[] capturedUserAgent = new String[1];
        int[] capturedTimeout = new int[1];
        int[] capturedRequestDelay = new int[1];
        Search.setDocumentFetcher((url, settings) -> {
            capturedUserAgent[0] = settings.getUserAgent();
            capturedTimeout[0] = settings.getTimeout();
            capturedRequestDelay[0] = settings.getRequestDelay();
            return documents.get(url);
        });

        PageGraph graph = SpiderGraph.crawler()
                .setUserAgent("SpiderTest/1.0")
                .setTimeout(3210)
                .setRequestDelay(700)
                .setMaxDepth(1)
                .setVerifyHost(true)
                .setUrlPrefix("https://example.com")
                .startSynchronousSearch("https://example.com/root");

        PageNode rootNode = graph.getNodes().get("https://example.com/root");
        PageNode childNode = graph.getNodes().get("https://example.com/allowed");

        assertEquals(2, graph.getNodes().size());
        assertEquals("Root", rootNode.getTitle());
        assertEquals("Allowed", childNode.getTitle());
        assertEquals("SpiderTest/1.0", capturedUserAgent[0]);
        assertEquals(3210, capturedTimeout[0]);
        assertEquals(700, capturedRequestDelay[0]);
        assertTrue(rootNode.getOutgoing().contains(childNode));
        assertTrue(childNode.getIncoming().contains(rootNode));
    }

    @Test
    public void startAsynchronousSearchReturnsSharedGraphWithoutRealNetworkCalls() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        Document rootDocument = html("https://example.com/root", "Async Root", "<a href='/child'>Child</a>");
        Document childDocument = html("https://example.com/child", "Async Child", "<p>Done</p>");
        documents.put("https://example.com/root", rootDocument);
        documents.put("https://example.com/child", childDocument);

        Search.setDocumentFetcher((url, settings) -> documents.get(url));

        PageGraph graph = SpiderGraph.crawler()
                .setMaxDepth(1)
                .startAsynchronousSearch("https://example.com/root");

        assertSame(graph, getSearchPageGraph());
        assertEquals(Set.of("https://example.com/root", "https://example.com/child"), graph.getNodes().keySet());
    }

    @Test
    public void startSynchronousSearchStopsWhenConfiguredHookRequestsIt() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        documents.put("https://example.com/root", html("https://example.com/root", "Root", "<a href='/child'>Child</a>"));
        documents.put("https://example.com/child", html("https://example.com/child", "Child", "<a href='/grandchild'>Grandchild</a>"));
        documents.put("https://example.com/grandchild", html("https://example.com/grandchild", "Grandchild", "<p>Done</p>"));

        AtomicInteger hookCalls = new AtomicInteger();
        Search.setDocumentFetcher((url, settings) -> documents.get(url));

        PageGraph graph = SpiderGraph.crawler()
                .setMaxDepth(2)
                .setCrawlStepHook(context -> hookCalls.incrementAndGet() < 2)
                .startSynchronousSearch("https://example.com/root");

        assertEquals(2, hookCalls.get());
        assertEquals(Set.of("https://example.com/root", "https://example.com/child"), graph.getNodes().keySet());
    }

    @Test
    public void startSynchronousSearchCanStopAfterCollectingFiftyNodes() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        for (int i = 0; i < 60; i++) {
            String currentUrl = "https://example.com/page-" + i;
            String body = i == 59
                    ? "<p>Last page</p>"
                    : "<a href='/page-" + (i + 1) + "'>Next</a>";
            documents.put(currentUrl, html(currentUrl, "Page " + i, body));
        }

        Search.setDocumentFetcher((url, settings) -> documents.get(url));

        PageGraph graph = SpiderGraph.crawler()
                .setMaxDepth(60)
                .setRequestDelay(0)
                .setCrawlStepHook(context -> context.graph().getNodes().size() < 50)
                .startSynchronousSearch("https://example.com/page-0");

        assertEquals(50, graph.getNodes().size());
        assertTrue(graph.getNodes().containsKey("https://example.com/page-49"));
        assertTrue(graph.getNodes().get("https://example.com/page-49").getTitle().contains("Page 49"));
        assertTrue(graph.getNodes().containsKey("https://example.com/page-0"));
    }

    @Test
    public void startSynchronousSearchCanStopWhenSpecificUrlIsVisited() throws Exception {
        Map<String, Document> documents = new HashMap<>();
        documents.put("https://example.com/root", html("https://example.com/root", "Root", "<a href='/child'>Child</a>"));
        documents.put("https://example.com/child", html("https://example.com/child", "Child", "<a href='/stop-here'>Stop</a>"));
        documents.put("https://example.com/stop-here", html("https://example.com/stop-here", "Stop", "<a href='/after-stop'>After</a>"));
        documents.put("https://example.com/after-stop", html("https://example.com/after-stop", "After", "<p>Done</p>"));

        Search.setDocumentFetcher((url, settings) -> documents.get(url));

        PageGraph graph = SpiderGraph.crawler()
                .setMaxDepth(4)
                .setRequestDelay(0)
                .setCrawlStepHook(context ->
                        !context.node().getUrl().equals("https://example.com/stop-here"))
                .startSynchronousSearch("https://example.com/root");

        assertEquals(Set.of(
                "https://example.com/root",
                "https://example.com/child",
                "https://example.com/stop-here"
        ), graph.getNodes().keySet());
        assertTrue(graph.getNodes().containsKey("https://example.com/stop-here"));
    }

    private static Document html(String url, String title, String body) {
        return Jsoup.parse("<html><head><title>" + title + "</title></head><body>" + body + "</body></html>", url);
    }

    private static void resetSearchState() throws Exception {
        PageGraph graph = getSearchPageGraph();
        graph.getNodes().clear();

        Field visitedField = Search.class.getDeclaredField("visited");
        visitedField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> visited = (Set<String>) visitedField.get(null);
        visited.clear();
    }

    private static PageGraph getSearchPageGraph() throws Exception {
        Field pageGraphField = Search.class.getDeclaredField("pageGraph");
        pageGraphField.setAccessible(true);
        return (PageGraph) pageGraphField.get(null);
    }

    private static void resetCrawlerStatics() throws Exception {
        Field urlPrefixField = SpiderGraph.Crawler.class.getDeclaredField("urlPrefix");
        urlPrefixField.setAccessible(true);
        urlPrefixField.set(null, null);

        Field verifyRootHostField = SpiderGraph.Crawler.class.getDeclaredField("verifyRootHost");
        verifyRootHostField.setAccessible(true);
        verifyRootHostField.setBoolean(null, true);
    }
}
