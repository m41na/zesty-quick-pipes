package works.zesty.quick.rx;

import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

public class App {
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("ddd MMM yyyy '@' mm:hh:ss a");

	public static void main(final String[] args) {
        HttpServer
                .newServer(8080)
                .start((req, resp) -> {
                    String path = req.getDecodedPath().substring(1);
                    String time = sdf.format(new Date());
    				final String responseMessage = String.format("incoming request: %s - %s'", path, time);
    				
                    Observable<String> response = Observable.just(responseMessage);
                    return resp.writeString(response);
                })
                .awaitShutdown();
    }
}
