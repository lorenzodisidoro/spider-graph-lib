package com.narae.spidergraph.model;

import junit.framework.TestCase;

public class PageNodeTest extends TestCase {

    public void testShouldCreateNodeWithUrl() {
        PageNode node = new PageNode("https://example.com");

        assertEquals("https://example.com", node.getUrl());
        assertNull(node.getTitle());
        assertNull(node.getText());
        assertTrue(node.getOutgoing().isEmpty());
        assertTrue(node.getIncoming().isEmpty());
    }

    public void testShouldSetAndGetContent() {
        PageNode node = new PageNode("https://example.com");

        node.setContent("Example Title", "Example Text");

        assertEquals("Example Title", node.getTitle());
        assertEquals("Example Text", node.getText());
    }

    public void testShouldAddOutgoingNode() {
        PageNode source = new PageNode("https://source.com");
        PageNode outgoing = new PageNode("https://outgoing.com");

        source.setOutgoing(outgoing);

        assertEquals(1, source.getOutgoing().size());
        assertTrue(source.getOutgoing().contains(outgoing));
    }

    public void testShouldAddIncomingNode() {
        PageNode source = new PageNode("https://source.com");
        PageNode incoming = new PageNode("https://incoming.com");

        source.setIncoming(incoming);

        assertEquals(1, source.getIncoming().size());
        assertTrue(source.getIncoming().contains(incoming));
    }
}