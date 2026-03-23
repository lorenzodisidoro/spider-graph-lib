package com.narae.spidergraph.crawler;

import com.narae.spidergraph.model.CrawlerSettings;
import com.narae.spidergraph.model.PageGraph;
import com.narae.spidergraph.model.PageNode;
import com.narae.spidergraph.utils.HtmlFetcher;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Internal crawler entry point used to traverse pages and build a {@link PageGraph}.
 * <p>
 * This class exposes both asynchronous and synchronous depth-first traversal strategies.
 * During the crawl it fetches each page, stores its title and text content in the
 * corresponding {@link PageNode}, and connects discovered links as graph edges.
 * </p>
 * <p>
 * The crawler state is shared through static fields, so the same visited set and graph
 * instance are reused for the lifetime of the JVM unless cleared by higher-level code.
 * </p>
 */
public class Search {
    @FunctionalInterface
    interface DocumentFetcher {
        Document fetch(String url, CrawlerSettings settings);
    }

    /**
     * Shared graph populated while the crawler discovers new pages.
     */
    private static final PageGraph pageGraph = new PageGraph();

    /**
     * Tracks URLs that have already been processed to prevent cycles and duplicate work.
     */
    private static final Set<String> visited = ConcurrentHashMap.newKeySet();

    /**
     * Logger used to trace crawl progress.
     */
    private static final Logger logger = LoggerFactory.getLogger(Search.class);

    /**
     * Fetch strategy used by sync and async traversals.
     */
    private static DocumentFetcher documentFetcher = Search::fetchWithHtmlFetcher;

    /**
     * Shared throttle state used to enforce a minimum delay between all crawler requests.
     */
    private static final Object requestThrottleLock = new Object();
    private static long lastRequestTimestamp;
    private static Sleeper sleeper = Thread::sleep;
    private static LongSupplier clock = System::currentTimeMillis;

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    /**
     * Crawls a page asynchronously and recursively follows its links until the configured
     * depth is reached.
     *
     * @param url the absolute URL to crawl
     * @param depth the current traversal depth, where {@code 0} is the starting page
     * @param settings crawler configuration such as depth, timeout, user agent, and filters
     * @return a future completed with the shared {@link PageGraph} once the crawl branch finishes
     */
    static CompletableFuture<PageGraph> async(String url, int depth, CrawlerSettings settings) {
        // if already visited or maxDepth is reached stop
        if (visited.contains(url) || depth > settings.getMaxDepth()) {
            return CompletableFuture.completedFuture(pageGraph);
        }

        visited.add(url);

        CompletableFuture<Document> documentFuture = CompletableFuture.supplyAsync(() -> throttledFetch(url, settings));

        return documentFuture.thenCompose(document -> {
            if (document == null) {
                return CompletableFuture.completedFuture(null);
            }

            logger.info("Get or create page node in async method and set the page content for {}", url);
            PageNode currentPageNote = pageGraph.getOrCreate(url);
            currentPageNote.setContent(document.title(), document.text());

            if (depth == settings.getMaxDepth()) {
                return CompletableFuture.completedFuture(null);
            }

            // Fetches all links present on the current page
            Elements links = document.select("a[href]");

            List<CompletableFuture<PageGraph>> futures = new ArrayList<>();

            for (Element link : links) {
                String currentUrl = getAndVerifyUrl(settings, link);

                if (currentUrl == null)
                    continue;

                // Links from the currentPageNode -> target and vice versa
                PageNode target = pageGraph.getOrCreate(currentUrl);
                currentPageNote.setOutgoing(target);
                target.setIncoming(currentPageNote);

                // Run dfsAsync recursively on current URL
                if (!visited.contains(currentUrl)) {
                    futures.add(async(currentUrl, depth + 1, settings));
                }
            }

            // It is used to create a single CompletableFuture that completes only when all the child tasks have finished.
            return CompletableFuture
                    .allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> pageGraph);
        });
    }

    /**
     * Crawls a page synchronously and recursively follows its links until the configured
     * depth is reached.
     *
     * @param url the absolute URL to crawl
     * @param depth the current traversal depth, where {@code 0} is the starting page
     * @param settings crawler configuration such as depth, timeout, user agent, and filters
     * @return the shared {@link PageGraph} populated during the crawl
     */
    static PageGraph sync(String url, int depth, CrawlerSettings settings) {
        if (visited.contains(url) || depth > settings.getMaxDepth()) return pageGraph;

        visited.add(url);

        Document document = throttledFetch(url, settings);

        if (document == null) return pageGraph;

        logger.info("Get or create page node in sync method and set the page content for {}", url);
        PageNode currentNode = pageGraph.getOrCreate(url);
        currentNode.setContent(document.title(), document.text());

        if (depth == settings.getMaxDepth()) return pageGraph;

        Elements links = document.select("a[href]");

        for (Element link : links) {
            String currentUrl = getAndVerifyUrl(settings, link);

            if (currentUrl == null)
                continue;

            PageNode target = pageGraph.getOrCreate(currentUrl);
            currentNode.setOutgoing(target);
            target.setIncoming(currentNode);

            return sync(currentUrl, depth + 1, settings);
        }

        return pageGraph;
    }

    private static String getAndVerifyUrl(CrawlerSettings settings, Element link) {
        String currentUrl = link.absUrl("href");
        if (currentUrl.isEmpty())
            return null;

        if (settings.isVerifyRootHost() && isSameDomainOfRootHost(currentUrl, settings.getRootHost()))
            return null;

        if (settings.getUrlPrefix() != null && !currentUrl.startsWith(settings.getUrlPrefix()))
            return null;

        return currentUrl;
    }

    /**
     * Checks whether a URL belongs to the configured root host.
     *
     * @param url the URL to validate
     * @param rootHost the root host configured for the crawl
     * @return {@code true} when the URL host is missing, invalid, or different from the root host
     */
    private static boolean isSameDomainOfRootHost(String url, String rootHost) {
        try {
            String host = URI.create(url).getHost();
            return host == null || !host.equalsIgnoreCase(rootHost);
        } catch (Exception e) {
            return true;
        }
    }

    static void setDocumentFetcher(DocumentFetcher fetcher) {
        documentFetcher = fetcher;
    }

    static void resetDocumentFetcher() {
        documentFetcher = Search::fetchWithHtmlFetcher;
    }

    static void setSleeper(Sleeper newSleeper) {
        sleeper = newSleeper;
    }

    static void resetSleeper() {
        sleeper = Thread::sleep;
    }

    static void setClock(LongSupplier newClock) {
        clock = newClock;
    }

    static void resetClock() {
        clock = System::currentTimeMillis;
    }

    static void resetRequestThrottle() {
        synchronized (requestThrottleLock) {
            lastRequestTimestamp = 0L;
        }
    }

    private static Document throttledFetch(String url, CrawlerSettings settings) {
        applyRequestThrottle(settings.getRequestDelay());
        return documentFetcher.fetch(url, settings);
    }

    private static void applyRequestThrottle(int requestDelay) {
        if (requestDelay <= 0) {
            return;
        }

        synchronized (requestThrottleLock) {
            long now = clock.getAsLong();
            long waitTime = lastRequestTimestamp + requestDelay - now;

            if (waitTime > 0) {
                try {
                    sleeper.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Crawler interrupted while throttling requests");
                    return;
                }
                now = clock.getAsLong();
            }

            lastRequestTimestamp = now;
        }
    }

    private static Document fetchWithHtmlFetcher(String url, CrawlerSettings settings) {
        return HtmlFetcher.connector()
                .userAgent(settings.getUserAgent())
                .timeout(settings.getTimeout())
                .fetch(url);
    }
}
