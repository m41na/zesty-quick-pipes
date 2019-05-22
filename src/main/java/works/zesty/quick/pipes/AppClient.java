package works.zesty.quick.pipes;

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpMethod;

import com.google.gson.Gson;

import works.zesty.quick.pipes.App.SearchContext;

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
		SearchContext results = sendRequest(httpClient, url[1]);
		System.out.printf("%d search results%n", results);
		
		System.out.printf("took %d milliseconds%n", TimeUnit.NANOSECONDS.toMillis((System.nanoTime() - start)));
		// Stop HttpClient
		httpClient.stop();
	}

	public static SearchContext sendRequest(HttpClient httpClient, String href) throws Exception {
		ContentResponse response = httpClient.newRequest(hostUrl + href).method(HttpMethod.GET).agent("zesty-router client")
				.send();
		if (response.getStatus() == 200) {
			return gson.fromJson(response.getContentAsString(), SearchContext.class);
		}
		return new SearchContext(href, 0);
	}
}
