package com.narae.spidergraph.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static com.narae.spidergraph.utils.Constants.DEFAULT_TIMEOUT;
import static com.narae.spidergraph.utils.Constants.DEFAULT_USER_AGENT;


/**
 * Utility class used to fetch and parse web pages.
 *
 * <p>This class provides a simple wrapper around {@link Jsoup} for retrieving
 * HTML documents from the web. It exposes a configurable {@link Connector}
 * that allows customization of parameters such as the HTTP user agent
 * and request timeout.</p>
 *
 * <p>Typical usage:</p>
 * <pre>
 * Document doc = HtmlFetcher.connector()
 *         .userAgent("MyCrawler/1.0")
 *         .timeout(10000)
 *         .fetch("www.example.com");
 * </pre>
 *
 * <p>If an error occurs during the request, {@code null} is returned and
 * the error is logged.</p>
 */
public class HtmlFetcher {

    /**
     * Configurable wrapper around {@link Jsoup#connect(String)} used to fetch documents.
     */
    public static class Connector {
        private final Logger logger = LoggerFactory.getLogger(Connector.class);
        private String userAgent = DEFAULT_USER_AGENT;
        private int timeout = DEFAULT_TIMEOUT;

        /**
         * Fetches the HTML document from the given URL.
         *
         * @param url the URL of the web page to retrieve
         * @return the parsed {@link Document} if the request succeed
         * or {@code null} if the URL is invalid or an error occurs
         */
        public Document fetch(String url) {
            Document document = null;

            if (url == null || url.isEmpty()) {
                logger.error("URL is null or empty");
                return document;
            }

            try {
                document = Jsoup.connect(url)
                        .userAgent(userAgent)
                        .timeout(timeout)
                        .get();
            } catch (IOException e) {
                logger.error("Error fetching URL: {}", url, e);
            }

            return document;
        }

        /**
         * Sets the HTTP user agent used for requests.
         *
         * @param newUserAgent the user agent header value
         * @return the current connector instance
         */
        public Connector userAgent(String newUserAgent) {
            userAgent = newUserAgent;
            return this;
        }

        /**
         * Sets the request timeout in milliseconds.
         *
         * @param newTimeout the maximum time allowed for the request
         * @return the current connector instance
         */
        public Connector timeout(int newTimeout) {
            timeout = newTimeout;
            return this;
        }
    }

    /**
     * Creates a connector with the library default user agent and timeout.
     *
     * @return a new connector instance
     */
    public static Connector connector() {
        return new Connector();
    }
}
