package com.guzt.starter.sse.service;


import com.guzt.starter.sse.properties.SseProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.util.concurrent.TimeUnit;


/**
 * SSE服务 server sent events
 *
 * @author <a href="mailto:gzt19881123@163.com">guzt</a>
 */
@SuppressWarnings("unused")
public class SseServer {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected SseProperties sseProperties;

    protected SseBusinessService sseBusinessService;

    EventLoopGroup bossGroup = new NioEventLoopGroup();
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    protected boolean started = false;

    public void init() {
        log.debug("SSE服务初始化完毕");
    }

    public SseProperties getSseProperties() {
        return sseProperties;
    }

    public void setSseProperties(SseProperties sseProperties) {
        this.sseProperties = sseProperties;
    }

    public SseBusinessService getSseBusinessService() {
        return sseBusinessService;
    }

    public void setSseBusinessService(SseBusinessService sseBusinessService) {
        this.sseBusinessService = sseBusinessService;
    }

    public void shutdown() {
        log.debug("SSE服务 Shutting down server...");
        // 优雅关闭 workerGroup
        if (!workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully(5, 10, TimeUnit.SECONDS);
        }
        // 优雅关闭 bossGroup
        if (!bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully(5, 10, TimeUnit.SECONDS);
        }
        log.debug("SSE服务 Server shut down gracefully.");
    }

    @Async
    public void start() throws Exception {
        if (started) {
            log.debug("SSE服务已经启动，无需再次启动");
            return;
        }
        log.debug("SSE服务正在启动...");
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(sseProperties.getMaxContentLength()));
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new SseHandler(sseProperties, sseBusinessService));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, sseProperties.getMaxTcpConnections())
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // 绑定端口并同步
            ChannelFuture f = b.bind(sseProperties.getPort()).sync();
            log.debug("SSE服务启动完成，绑定端口：{}", sseProperties.getPort());
            started = true;
            // 添加关闭钩子
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            // 等待服务器通道关闭
            f.channel().closeFuture().sync();
        } finally {
            shutdown();
        }
    }

}
