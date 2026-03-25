package com.narae.spidergraph.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a crawled page and its incoming and outgoing links.
 */
public class PageNode {
    private final String url;
    private String title;
    private String text;

    private final Set<PageNode> outgoing = ConcurrentHashMap.newKeySet();
    private final Set<PageNode> incoming = ConcurrentHashMap.newKeySet();

    /**
     * Creates a node identified by its absolute URL.
     *
     * @param url the page URL
     */
    public PageNode(String url) {
        this.url = url;
    }

    /**
     * Returns the absolute URL of this page.
     *
     * @return the page URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the parsed HTML title for this page.
     *
     * @return the page title, or {@code null} if not populated yet
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the extracted text content for this page.
     *
     * @return the page text, or {@code null} if not populated yet
     */
    public String getText() {
        return text;
    }

    /**
     * Updates the parsed content stored for this page.
     *
     * @param title the page title
     * @param text the extracted page text
     */
    public void setContent(String title, String text) {
        this.title = title;
        this.text = text;
    }

    /**
     * Returns the set of pages linked from this page.
     *
     * @return the outgoing links
     */
    public Set<PageNode> getOutgoing() { return outgoing; }

    /**
     * Returns the set of pages linking to this page.
     *
     * @return the incoming links
     */
    public Set<PageNode> getIncoming() { return incoming; }

    /**
     * Adds an outgoing link from this page to the provided node.
     *
     * @param node the linked target page
     */
    public void setOutgoing(PageNode node) { outgoing.add(node); }

    /**
     * Adds an incoming link to this page from the provided node.
     *
     * @param node the source page linking here
     */
    public void setIncoming(PageNode node) { incoming.add(node); }
}
