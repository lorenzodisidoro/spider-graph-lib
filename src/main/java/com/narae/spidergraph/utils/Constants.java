package com.narae.spidergraph.utils;

/**
 * Contains values.
 *
 * <p>
 * This class is not meant to be instantiated and only provides
 * static constant fields used throughout the application.
 * </p>
 */
public final class Constants {
    /** Default timeout for HTTP connections (in milliseconds) */
    public static final int DEFAULT_MAX_DEPTH = 1;

    /** Default timeout for HTTP connections (in milliseconds) */
    public static final int DEFAULT_TIMEOUT = 5000;

    /** Default request delay for HTTP request (in milliseconds) */
    public static final int DEFAULT_REQUEST_DELAY = 1000;

    /** Default user agent for HTTP connections */
    public static final String DEFAULT_USER_AGENT = "CrawlWebGraph/1.0";

    /** Default verify roothost */
    public static final boolean DEFAULT_VERIFY_ROOT_HOST = true;
}
