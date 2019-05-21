package works.zesty.quick.netty;

import static io.netty.buffer.Unpooled.copiedBuffer;

import java.util.Date;

import com.ibm.icu.text.SimpleDateFormat;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;

public class App {

	private ChannelFuture channel;
	private final EventLoopGroup masterGroup;
	private final EventLoopGroup slaveGroup;
	private final SimpleDateFormat sdf = new SimpleDateFormat("ddd MMM yyyy '@' mm:hh:ss a");

	public App() {
		masterGroup = new NioEventLoopGroup();
		slaveGroup = new NioEventLoopGroup();
	}

	public void start() // #1
	{
		Runtime.getRuntime().addShutdownHook(new Thread( () -> shutdown()));

		try {
			// #3
			final ServerBootstrap bootstrap = new ServerBootstrap()
					.group(masterGroup, slaveGroup)
					.channel(NioServerSocketChannel.class)
					.option(ChannelOption.SO_BACKLOG, 10000)
					.childOption(ChannelOption.SO_KEEPALIVE, true)
					.childHandler(new HttpInitializer(null));
			channel = bootstrap.bind(8080).sync();
		} catch (final InterruptedException e) {
		}
	}

	public void shutdown() // #2
	{
		slaveGroup.shutdownGracefully();
		masterGroup.shutdownGracefully();

		try {
			channel.channel().closeFuture().sync();
		} catch (InterruptedException e) {
		}
	}

	public static void main(String[] args) {
		new App().start();
	}
	
	class HttpInitializer extends ChannelInitializer<SocketChannel> {
		
		 private final SslContext sslCtx;

	    HttpInitializer(SslContext sslCtx) {
	        this.sslCtx = sslCtx;
	    }
		    
		@Override
		public void initChannel(final SocketChannel ch) throws Exception {
			ChannelPipeline p = ch.pipeline();
			if (sslCtx != null) {
	            p.addLast(sslCtx.newHandler(ch.alloc()));
	        }
			ch.pipeline().addLast("codec", new HttpServerCodec());
			ch.pipeline().addLast("aggregator", new HttpObjectAggregator(512 * 1024));
			ch.pipeline().addLast("request", new PingHandler()); // #5
		}
	}
	
	@ChannelHandler.Sharable
	class PingHandler extends ChannelInboundHandlerAdapter {
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof FullHttpRequest) {
				final FullHttpRequest request = (FullHttpRequest) msg;

				String time = sdf.format(new Date());
				final String responseMessage = String.format("incoming request: %s'", time);

				FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
						HttpResponseStatus.OK, copiedBuffer(responseMessage.getBytes()));

				if (HttpUtil.isKeepAlive(request)) {
					response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
				}
				response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
				response.headers().set(HttpHeaderNames.CONTENT_LENGTH, responseMessage.length());

				ctx.write(response, ctx.voidPromise());
			} else {
				super.channelRead(ctx, msg);
			}
		}

		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			ctx.flush();
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
				throws Exception {
			ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
					HttpResponseStatus.INTERNAL_SERVER_ERROR,
					copiedBuffer(cause.getMessage().getBytes())));
		}
	}
}
