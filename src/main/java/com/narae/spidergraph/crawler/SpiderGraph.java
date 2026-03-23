package com.narae.spidergraph.crawler;

import com.narae.spidergraph.model.CrawlerSettings;
import com.narae.spidergraph.model.PageGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static com.narae.spidergraph.utils.Constants.*;

/**
 * Main entry point for creating and configuring a web crawler that produces a {@link PageGraph}.
 * <p>
 * Use {@link #crawler()} to obtain a fluent {@link Crawler} instance, customize the crawl settings,
 * and start either a synchronous or asynchronous crawl from a root URL.
 */
public class SpiderGraph {

    /**
     * Fluent entry point used to configure and start a crawl.
     */
    public static class Crawler {
        private String rootHost;
        private static String urlPrefix;
        private static boolean verifyRootHost = DEFAULT_VERIFY_ROOT_HOST;
        private String userAgent = DEFAULT_USER_AGENT;
        private int timeout = DEFAULT_TIMEOUT;
        private int maxDepth = DEFAULT_MAX_DEPTH;
        private int requestDelay = DEFAULT_REQUEST_DELAY;
        private final Logger logger = LoggerFactory.getLogger(Crawler.class);

        /**
         * Starts a synchronous crawl from the provided URL.
         *
         * @param url the absolute URL used as the crawl entry point
         * @return the graph generated from the crawled pages
         */
        public PageGraph startSynchronousSearch(String url) {
            rootHost = URI.create(url).getHost();
            logger.info("Start sequential crawler from URL {}", url);

            return Search.sync(url, 0, createDefaultSettings());
        }

        /**
         * Starts an asynchronous crawl from the provided URL and waits for completion.
         *
         * @param url the absolute URL used as the crawl entry point
         * @return the graph generated from the crawled pages
         */
        public PageGraph startAsynchronousSearch(String url) {
            rootHost = URI.create(url).getHost();
            logger.info("Start crawler from URL {}", url);

            CompletableFuture<PageGraph> rootFuture = Search.async(url, 0, createDefaultSettings());
            return rootFuture.join();
        }

        private CrawlerSettings createDefaultSettings() {
            CrawlerSettings settings = new CrawlerSettings();

            settings.setVerifyRootHost(verifyRootHost);
            settings.setUserAgent(userAgent);
            settings.setMaxDepth(maxDepth);
            settings.setTimeout(timeout);
            settings.setRootHost(rootHost);
            settings.setUrlPrefix(urlPrefix);
            settings.setRequestDelay(requestDelay);

            logger.info("Configurations timeout={}, userAgent={} and maxDepth={}", timeout, userAgent, maxDepth);
            logger.info("Root host={}", rootHost);

            return  settings;
        }

        /**
         * Sets the delay, in milliseconds, between crawl requests.
         *
         * @param requestDelay the delay to apply between requests
         * @return the current crawler instance
         */
        public Crawler setRequestDelay(int requestDelay) {
            logger.info("Setting request delay={}", requestDelay);
            this.requestDelay = requestDelay;
            return this;
        }

        /**
         * Restricts discovered URLs to those matching the provided prefix.
         *
         * @param prefix the URL prefix accepted by the crawler
         * @return the current crawler instance
         */
        public Crawler setUrlPrefix(String prefix) {
            urlPrefix = prefix;
            logger.info("Set URL prefix filter={}", prefix);
            return this;
        }

        /**
         * Enables or disables validation against the root host.
         *
         * @param verifyHost {@code true} to keep the crawl on the same host, {@code false} otherwise
         * @return the current crawler instance
         */
        public Crawler setVerifyHost(boolean verifyHost) {
            verifyRootHost = verifyHost;
            logger.info("Set verifyRootHost={}", verifyHost);
            return this;
        }

        /**
         * Sets the maximum crawl depth starting from the root URL.
         *
         * @param maxDepth the maximum number of link levels to traverse
         * @return the current crawler instance
         */
        public Crawler setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
            logger.info("Set maxDepth={}", maxDepth);
            return this;
        }

        /**
         * Sets the request timeout, in milliseconds.
         *
         * @param timeout the maximum time allowed for an HTTP request
         * @return the current crawler instance
         */
        public Crawler setTimeout(int timeout) {
            this.timeout = timeout;
            logger.info("Set timeout={}", timeout);
            return this;
        }

        /**
         * Sets the HTTP user agent sent with crawl requests.
         *
         * @param userAgent the user agent header value
         * @return the current crawler instance
         */
        public Crawler setUserAgent(String userAgent) {
            this.userAgent = userAgent;
            logger.info("Set userAgent={}", userAgent);
            return this;
        }
    }

    /**
     * Creates a new crawler instance with default settings.
     *
     * @return a configurable crawler
     */
    public static Crawler crawler() {
        return new Crawler();
    }
}
