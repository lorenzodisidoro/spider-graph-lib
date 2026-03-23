package com.narae.spidergraph.model;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PageNode {
    private final String url;
    private String title;
    private String text;

    private final Set<PageNode> outgoing = ConcurrentHashMap.newKeySet();
    private final Set<PageNode> incoming = ConcurrentHashMap.newKeySet();

    public PageNode(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public void setContent(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public Set<PageNode> getOutgoing() { return outgoing; }
    public Set<PageNode> getIncoming() { return incoming; }

    public void setOutgoing(PageNode node) { outgoing.add(node); }
    public void setIncoming(PageNode node) { incoming.add(node); }
}
