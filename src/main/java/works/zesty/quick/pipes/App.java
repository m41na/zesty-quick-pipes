package works.zesty.quick.pipes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.SimpleDateFormat;
import com.practicaldime.zesty.app.AppServer;
import com.practicaldime.zesty.servlet.RequestContext;

import works.zesty.quick.pipes.App.SearchResults;

public class App {
	
	private static final Logger LOG = LoggerFactory.getLogger(App.class);
	private static final SimpleDateFormat sdf = new SimpleDateFormat("ddd MMM yyyy '@' mm:hh:ss a");

	public static void main(String[] args) {
		int port = 1337;
		String host = "localhost";

		Map<String, String> props = new HashMap<>();
		props.put("appctx", "/");
		props.put("assets", "www");
		props.put("maxThreads", "10");
		props.put("maxConn", "1000");
		
		AppServer router = new AppServer(props).router();
		router
		.get("/ping", "", "", (RequestContext context) -> {
			String time = sdf.format(new Date());
			context.getResp().send(String.format("incoming request: '%s - %s'", context.getReq().getRequestURI(), time));			
			return CompletableFuture.completedFuture(context);
		})
		.get("/crawl", "", "", (RequestContext context) -> {
			Config config = new Config();
			config.put("target", new Value(context.getReq().param("target"), "https://www.nytimes.com/"));
			config.put("phrase", new Value(context.getReq().param("phrase"), "Travel"));
			config.put("depth", new Value(context.getReq().param("depth"), "2"));
			
			String url = config.get("target").value;
			
			try {
				Long start = System.nanoTime();
				CompletableFuture<SearchResults> future = crawlAndSearch(url, config.get("phrase").value, config.get("depth").getInt());
				SearchResults results = future.join();
				results.time = TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start));
				context.getResp().json(results);
				return CompletableFuture.completedFuture(context);
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
				return CompletableFuture.failedFuture(ex);
			}		
		}).listen(port, host, (msg) -> {
			LOG.info(msg);
		});
	}

	public static CompletableFuture<SearchResults> crawlAndSearch(String url, String phrase, int depth) {
		return CompletableFuture.supplyAsync(() -> {
			SearchContext search = new SearchContext(url, 1);
			search.doc = loadDoc(url);
			return search;
		})
		.exceptionally(th -> {
			th.printStackTrace(System.err);
			return null;
		})
		.thenApply(res -> {
			String content = readContent(res.doc);
			res.searchMatches = findMatches(content, phrase);
			return res;			
		})
		.thenApply(res -> {
			res.linksFound = findLinksHref(res.doc);
			return res;
		})
		.thenApplyAsync(res -> {
			System.out.printf("Found %d distinct links in '%s'%n", res.linksFound.size(), res.doc.baseUri());
			int count = res.searchMatches.size();
			if (count > 0) {
				System.out.printf("phrase matched %d times on %s%n", count, res.doc.baseUri());
				res.searchMatches.stream().forEach(match -> {
					System.out.printf("=> %s%n", match);
				});
			} else {
				System.out.println("=> no matching phrases were found");
			}

			int newDepth = depth - 1;	
			if(newDepth == 0) {
				SearchResults retVal = new SearchResults();
				if(res != null) {
					retVal.linksFound.put(res.url, res.linksFound);
					retVal.searchMatches.put(res.url, res.searchMatches);
				}
				return  retVal;
			}
			
			//crawl links found on this url
			res.doc = null;
			res.searchMatches = null;
			CompletableFuture<SearchResults> newRes = res.linksFound.stream().map(href -> crawlAndSearch(href, phrase, newDepth).handle((ctx, th) -> {
				if(th != null) {
					System.out.println("************ " + th.getMessage() + " ****************");
				}
				return ctx;
			})).reduce((CompletableFuture<SearchResults> t, CompletableFuture<SearchResults> u) -> {
				SearchResults aThis = (t.join() != null)? t.join() : new SearchResults();
				if(u.join() != null) {
					SearchResults aThat = u.join();
					aThis.linksFound.putAll(aThat.linksFound);
					aThis.searchMatches.putAll(aThat.searchMatches);
				}			
				return CompletableFuture.completedFuture(aThis);			
			}).get();
			return newRes.join();
		});
	}
	
	static class Config {
		
		private static final Map<String, Value> config = new HashMap<>();
		
		public Value get(String key) {
			return (config.containsKey(key))? config.get(key) : new Value(null, null);			
		}
		
		public Value put(String key, Value value) {
			return config.put(key, value);
		}
		
		public Value put(String key, String value, String altVal) {
			return config.put(key, new Value(value, altVal));
		}
	}

	static class Value {

		private final String value;

		public Value(String value, String alt) {
			super();
			this.value = value != null? value : alt;
		}

		public Boolean getBool() {
			return Boolean.valueOf(value != null? value : "false");
		}

		public Integer getInt() {
			return value != null && value.matches("\\d+") ? Integer.valueOf(value) : 2;
		}
	}

	static class SearchContext {

		public final String url;
		public final int depth;
		public Document doc;
		public Map<String, Optional<SearchContext>> linksFound = new HashMap<>();
		public List<String> searchMatches;
		
		public SearchContext(String url, int depth) {
			super();
			this.url = url;
			this.depth = depth;
		}
	}
	
	static class SearchResults {
		public Long time;
		public Map<String, Set<String>> linksFound = new HashMap<>();
		public Map<String, List<String>> searchMatches = new HashMap<>();
	}
	
	public static Document loadDoc(String url) { //Cherio - JS equivalent of Jsoup
		try {
			return Jsoup.connect(url).get();
		} catch (IOException e) {
			LOG.error("Could not parse this url: {}", url);
			e.printStackTrace(System.err);
			return null;
		}
	}

	public static List<Element> findLinks(Document doc) {
		List<Element> links = new ArrayList<>();
		Elements elements = doc.select("a");
		for (Element link : elements) {
			links.add(link);
		}
		return links;
	}

	public static Map<String, Optional<SearchContext>> findLinksHref(Document doc) {
		Map<String, Optional<SearchContext>> linksHref = new HashMap<>();
		if(doc != null) {
			findLinks(doc).stream().forEach(link -> {
				linksHref.put(link.absUrl("href"), Optional.empty());
			});
		}
		return linksHref;
	}

	public static String readContent(Document doc) {
		if (doc != null) {
			String text = doc.body().text();
			return text;
		}
		return "";
	}

	public static List<String> findMatches(String content, String phrase) {
		List<String> matches = new ArrayList<>();
		Pattern pattern = Pattern.compile("(\\S+\\s?\\b){1}" + phrase + "(\\b\\s.+?\\b){1}", Pattern.DOTALL); //account for special characters in phrase
		Matcher matcher = pattern.matcher(content);
		while (matcher.find()) {
			String match = matcher.group();
			matches.add(match);
		}
		return matches;
	}
}
