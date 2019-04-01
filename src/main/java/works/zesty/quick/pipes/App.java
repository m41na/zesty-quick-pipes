package works.zesty.quick.pipes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.SimpleDateFormat;
import com.practicaldime.zesty.app.AppServer;
import com.practicaldime.zesty.servlet.RequestContext;

public class App {
	private static final Logger LOG = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		int port = 1337;
		String host = "localhost";

		Map<String, String> props = new HashMap<>();
		props.put("appctx", "/");
		props.put("assets", "www");
		
		AppServer router = new AppServer(props).router();
		router.get("/check", "", "", (RequestContext context) -> {
			context.getResp().send(String.format("incoming request: '%s'", context.getReq().getRequestURI()));			
			return CompletableFuture.completedFuture(context);
		})
		.get("/date", "", "", (RequestContext context) -> {
			context.getResp().send(String.format("Date/Time now: '%s'", new SimpleDateFormat().format(new Date())));			
			return CompletableFuture.completedFuture(context);
		}).listen(port, host, (msg) -> {
			LOG.info(msg);
		});
	}

}
