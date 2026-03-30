package com.narae.spidergraph.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a crawled page and its incoming and outgoing links.
 */
@Getter
@Setter
public class PageNode {
    private final String url;
    private String title;
    private String text;
    private String html;

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

    /**
     * Updates the parsed content stored for this page.
     *
     * @param title the page title
     * @param text the extracted page text
     * @param html page content in HTML format
     */
    public void setContent(String title, String text, String html) {
        this.title = title;
        this.text = text;
        this.html = html;
    }
}
