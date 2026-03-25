package com.narae.spidergraph.crawler;

import com.narae.spidergraph.model.PageGraph;
import com.narae.spidergraph.model.PageNode;

/**
 * Immutable context passed to the crawl-step predicate after each parsed page.
 */
public record CrawlStepContext(PageNode node, int depth, PageGraph graph) {
}
