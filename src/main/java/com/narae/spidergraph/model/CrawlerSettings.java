package com.narae.spidergraph.model;

import lombok.Setter;
import lombok.Getter;

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
}
