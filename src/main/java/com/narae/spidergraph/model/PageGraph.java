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

    public PageNode getOrCreate(String url) {
        return nodes.computeIfAbsent(url, PageNode::new);
    }

    public Map<String, PageNode> getNodes() {
        return nodes;
    }

}
