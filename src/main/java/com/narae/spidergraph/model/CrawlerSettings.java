package com.narae.spidergraph.model;

import com.narae.spidergraph.crawler.CrawlStepContext;
import lombok.Setter;
import lombok.Getter;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Mutable crawler configuration shared across a crawl run.
 */
@Getter
@Setter
public class CrawlerSettings {
    private int maxDepth;
    private String userAgent;
    private int timeout;
    private String rootHost;
    private String urlPrefix;
    private boolean verifyRootHost;
    private int requestDelay;
    private Predicate<CrawlStepContext> crawlStepHook;
    private AtomicBoolean stopRequested = new AtomicBoolean(false);
}
