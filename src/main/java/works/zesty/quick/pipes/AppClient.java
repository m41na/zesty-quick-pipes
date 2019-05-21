package works.zesty.quick.pipes;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;

import com.google.gson.Gson;

import works.zesty.quick.pipes.App.SearchResults;

public class AppClient {

	private static Gson gson = new Gson();
	private static String hostUrl = "http://localhost:1337/crawl?depth=1&target=";
	private static String[] url = {"https://www.nytimes.com/","https://www.msn.com/en-us"};

	public static void main(String[] args) throws Exception {
		// Instantiate HttpClient
		HttpClient httpClient = new HttpClient();

		// Configure HttpClient, for example:
		httpClient.setFollowRedirects(false);

		// Start HttpClient
		httpClient.start();

		Long start = System.nanoTime();
		Set<String> links = sendRequest(httpClient, url[1]);
		System.out.printf("%d links found%n", links.size());
		System.out.println(links);
		
		//iterate results
		for (Iterator<String> iter = links.iterator(); iter.hasNext();) {
			Set<String> newLinks = sendRequest(httpClient, iter.next());
			System.out.printf("%d links found%n", newLinks.size());
			System.out.println(newLinks);
		}
		
		System.out.printf("took %d milliseconds%n", TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start)));
		// Stop HttpClient
		httpClient.stop();
	}

	public static Set<String> sendRequest(HttpClient httpClient, String href) throws Exception {
		ContentResponse response = httpClient.newRequest(hostUrl + href).method(HttpMethod.GET).agent("zesty-router client")
				.send();
		if (response.getStatus() == 200) {
			SearchResults res = gson.fromJson(response.getContentAsString(), SearchResults.class);
			return res.linksFound.values().stream().flatMap(Collection::stream)
				      .collect(Collectors.toSet());  
		}
		return Collections.emptySet();
	}
}
