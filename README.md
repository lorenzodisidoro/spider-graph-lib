# Spider Graph Lib

`spider-graph-lib` is a small Java library for crawling a website and building an in-memory graph of pages and links.

Each crawled page becomes a `PageNode`, and the full crawl result is exposed as a `PageGraph`. The library supports both synchronous and asynchronous traversal APIs and uses `jsoup` to fetch and parse HTML.

## Features

- Crawl from a root URL and build a graph of discovered pages
- Store page title and extracted text for each node
- Track incoming and outgoing links between pages
- Configure crawl depth, timeout, user agent, host validation, and URL prefix filtering
- Throttle requests with a shared delay between consecutive fetches
- Run the crawl synchronously or asynchronously

## Requirements

- Java 17+
- Maven 3.8+

## Installation

### Local build

Build the project with Maven:

```bash
mvn clean package
```

The generated JAR is created under `target/`.

If you want to use the library from another Maven project, install it locally:

```bash
mvn clean install
```

Then reference it from your project:

```xml
<dependency>
  <groupId>com.narae.spidergraph</groupId>
  <artifactId>spider-graph-lib</artifactId>
  <version>1.0.3</version>
</dependency>
```

### Via JitPack

The library can be consumed through JitPack.

1. Push the repository to GitHub.
2. Create and push a release tag, for example `v1.0.3`.
3. Make sure the project is publicly available.

Add the JitPack repository to the consumer project:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>
```

Then add the dependency:

```xml
<dependency>
  <groupId>com.github.lorenzodisidoro</groupId>
  <artifactId>spider-graph-lib</artifactId>
  <version>v1.0.3</version>
</dependency>
```
Open [jitpack.io](https://jitpack.io/#lorenzodisidoro/spider-graph-lib) for more details.

Release commands:

```bash
git tag v1.0.3
git push origin main --tags
```

## Quick Start

### Synchronous crawl

```java
import com.narae.spidergraph.crawler.SpiderGraph;
import com.narae.spidergraph.model.PageGraph;
import com.narae.spidergraph.model.PageNode;

PageGraph graph = SpiderGraph.crawler()
        .setMaxDepth(2)
        .setTimeout(5000)
        .setRequestDelay(1000)
        .setUserAgent("MyCrawler/1.0")
        .setVerifyHost(true)
        .setUrlPrefix("https://example.com")
        .startSynchronousSearch("https://example.com");

for (PageNode node : graph.getNodes().values()) {
    System.out.println(node.getUrl());
    System.out.println(node.getTitle());
    System.out.println(node.getOutgoing().size());
}
```

### Asynchronous crawl

```java
import com.narae.spidergraph.crawler.SpiderGraph;
import com.narae.spidergraph.model.PageGraph;

PageGraph graph = SpiderGraph.crawler()
        .setMaxDepth(2)
        .setRequestDelay(1000)
        .startAsynchronousSearch("https://example.com");
```

## Public API

### `SpiderGraph`

Main entry point for creating a crawler:

```java
SpiderGraph.Crawler crawler = SpiderGraph.crawler();
```

Available configuration methods:

- `setMaxDepth(int maxDepth)`
- `setTimeout(int timeout)`
- `setUserAgent(String userAgent)`
- `setVerifyHost(boolean verifyHost)`
- `setUrlPrefix(String prefix)`
- `setRequestDelay(int requestDelay)`
- `setCrawlStepHook(Predicate<CrawlStepContext> hook)`

Execution methods:

- `startSynchronousSearch(String url)`
- `startAsynchronousSearch(String url)`

### `PageGraph`

Represents the crawl result as a thread-safe map of URL -> `PageNode`.

Main method:

- `getNodes()`

### `PageNode`

Represents a single crawled page.

Main properties and methods:

- `getUrl()`
- `getTitle()`
- `getText()`
- `getOutgoing()`
- `getIncoming()`

## Default Settings

Current defaults from the library:

- Max depth: `5`
- Timeout: `5000` ms
- Request delay: `1000` ms
- User agent: `CrawlWebGraph/1.0`
- Verify root host: `true`

## How Filtering Works

When `setVerifyHost(true)` is enabled, the crawler accepts only URLs that belong to the same host as the root URL.

When `setUrlPrefix(...)` is set, the crawler accepts only URLs starting with that prefix.

Both filters can be combined.

## Request Delay And Throttling

Use `setRequestDelay(...)` to define the minimum delay, in milliseconds, between two consecutive crawler requests.

This delay is enforced inside `Search` for both:

- `startSynchronousSearch(...)`
- `startAsynchronousSearch(...)`

In asynchronous mode, the throttle is shared across all running crawl tasks, so concurrent branches are still paced globally instead of sending bursts of requests at the same time.

## Stopping A Crawl Gracefully

Use `setCrawlStepHook(...)` to inspect each parsed page and stop the crawl without losing the graph collected so far.

```java
PageGraph graph = SpiderGraph.crawler()
        .setCrawlStepHook(context -> context.depth() < 2)
        .startSynchronousSearch("https://example.com");
```

The hook is invoked after each page has been fetched and parsed. Returning `false` stops the crawl and returns the partial graph.

Stop after collecting 50 nodes:

```java
PageGraph graph = SpiderGraph.crawler()
        .setCrawlStepHook(context -> context.graph().getNodes().size() < 50)
        .startSynchronousSearch("https://example.com");
```

Stop when a specific URL is visited:

```java
PageGraph graph = SpiderGraph.crawler()
        .setCrawlStepHook(context ->
                !context.node().getUrl().equals("https://example.com/stop-here"))
        .startSynchronousSearch("https://example.com");
```

## Development

Run tests:

```bash
mvn test
```

Build the package:

```bash
mvn package
```

## Current Limitations

- Crawl state is stored in static fields inside `Search`, so graph data and visited URLs are shared within the same JVM process.
- Request throttling state is also shared in static crawler state, so separate crawl runs in the same JVM affect each other's pacing.
- The current public API does not expose a reset method for crawler state between runs.
- The library does not currently include `robots.txt` handling, sitemap support, or persistence.
- The library logs through SLF4J; the host application is expected to provide the logging backend it prefers.

## Project Structure

```text
src/main/java/com/narae/spidergraph/
  crawler/    crawler entry points and traversal logic
  model/      graph and node data structures
  utils/      constants and HTML fetching utilities
```

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
