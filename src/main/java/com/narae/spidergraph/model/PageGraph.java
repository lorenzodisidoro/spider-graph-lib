package com.narae.spidergraph.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a graph of web pages.
 *
 * <p>
 * The {@code PageGraph} class maintains a collection of {@link PageNode} objects
 * indexed by their URL. Each node represents a web page in the graph and may
 * contain information such as links to other pages.
 * </p>
 *
 * <p>
 * Internally, the class uses a {@link java.util.concurrent.ConcurrentHashMap}
 * to ensure thread-safe access, allowing multiple threads to read and create
 * nodes concurrently without causing race conditions.
 * </p>
 */
public class PageGraph {
    private final Map<String, PageNode> nodes = new ConcurrentHashMap<>();

    /**
     * Returns the node associated with the URL, creating it if it does not exist yet.
     *
     * @param url the page URL used as key in the graph
     * @return the existing or newly created page node
     */
    public PageNode getOrCreate(String url) {
        return nodes.computeIfAbsent(url, PageNode::new);
    }

    /**
     * Returns the thread-safe map of crawled nodes keyed by URL.
     *
     * @return the graph nodes map
     */
    public Map<String, PageNode> getNodes() {
        return nodes;
    }

}
