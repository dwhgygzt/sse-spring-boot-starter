package com.guzt.starter.sse.service;

import com.guzt.starter.sse.pojo.dto.EventDTO;
import com.guzt.starter.sse.properties.EventEndpoint;
import com.guzt.starter.sse.properties.SseProperties;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.ScheduledFuture;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * SSE处理器
 *
 * @author <a href="mailto:gzt19881123@163.com">guzt</a>
 */
@SuppressWarnings("unused")
@ChannelHandler.Sharable
public class SseHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    // 定义一个 AttributeKey 用于存储 ScheduledFuture
    private static final AttributeKey<ScheduledFuture<?>> SCHEDULED_FUTURE_KEY = AttributeKey.valueOf("scheduledFuture");

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected SseProperties sseProperties;

    protected Map<String, EventEndpoint> eventEndpointMap = new HashMap<>(4);

    protected SseBusinessService sseBusinessService;

    public SseProperties getSseProperties() {
        return sseProperties;
    }

    private void setEventEndpointMap() {
        if (sseProperties.getEventEndpoints() == null || sseProperties.getEventEndpoints().isEmpty()) {
            sseProperties.setEventEndpoints(Collections.singletonList(new EventEndpoint()));
        }
        sseProperties.getEventEndpoints().forEach(eventEndpoint -> eventEndpointMap.put(eventEndpoint.getEndpoint(), eventEndpoint));
    }

    public void setSseProperties(SseProperties sseProperties) {
        this.sseProperties = sseProperties;
        setEventEndpointMap();
    }

    public SseBusinessService getSseBusinessService() {
        return sseBusinessService;
    }

    public void setSseBusinessService(SseBusinessService sseBusinessService) {
        this.sseBusinessService = sseBusinessService;
    }

    public SseHandler(SseProperties sseProperties, SseBusinessService sseBusinessService) {
        this.sseProperties = sseProperties;
        this.sseBusinessService = sseBusinessService;
        setEventEndpointMap();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 获取远程地址
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>SseHandler: channelActive, remoteAddress={}", remoteAddress);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 获取远程地址
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>SseHandler: channelInactive, remoteAddress={}", remoteAddress);
        // 从 ChannelHandlerContext 中获取定时任务并取消
        ScheduledFuture<?> scheduledFuture = ctx.channel().attr(SCHEDULED_FUTURE_KEY).get();
        if (scheduledFuture != null) {
            // 显式取消定时任务
            scheduledFuture.cancel(false);
        }
        super.channelInactive(ctx);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (request.method() == HttpMethod.OPTIONS) {
            // 处理预检请求
            FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS");
            response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
            ctx.writeAndFlush(response);
            return;
        }
        if (HttpUtil.is100ContinueExpected(request)) {
            send100Continue(ctx);
        }
        // 检查请求的 URI 是否以指定的前缀开始
        String fullUri = request.uri();
        int queryIndex = fullUri.indexOf('?');
        String pathWithoutQuery = (queryIndex != -1) ? fullUri.substring(0, queryIndex) : fullUri;
        if (!eventEndpointMap.containsKey(pathWithoutQuery)) {
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND));
            return;
        }
        // 解析 GET 参数
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(fullUri);
        Map<String, List<String>> parameters = queryStringDecoder.parameters();
        if (parameters != null && !parameters.isEmpty()) {
            log.debug(">>>>>>>>>>>>>>>>>>>>>>>>SseHandler: parameters={}", parameters);
        }
        // 创建一个空的 Map 用于存储请求头
        Map<String, String> headersMap = new HashMap<>(16);
        HttpHeaders headers = request.headers();
        headers.forEach(header -> headersMap.put(header.getKey(), header.getValue()));
        if (!sseBusinessService.connectAuth(pathWithoutQuery, headersMap, parameters)) {
            ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED));
            return;
        }
        // CORS 头, 允许所有域
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/event-stream");
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "no-cache");
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type");
        ctx.write(response);

        EventEndpoint endpoint = eventEndpointMap.get(pathWithoutQuery);
        // 判定是否有重试策略
        if (endpoint.getRetryMilliseconds() != null && endpoint.getRetryMilliseconds() > 0) {
            sendRetryEvent(ctx, endpoint);
        }
        // 定期发送 SSE 事件
        long initialDelay = 0L;
        long period = endpoint.getEventPeriodMilliseconds();
        ScheduledFuture<?> scheduledFuture = ctx.executor().scheduleAtFixedRate(
                () -> sendSseEvent(ctx, sseBusinessService.generateEvent(pathWithoutQuery, headersMap, parameters)),
                initialDelay, period, TimeUnit.MILLISECONDS);
        // 将定时任务的引用存储在 ChannelHandlerContext 的属性中
        ctx.channel().attr(SCHEDULED_FUTURE_KEY).set(scheduledFuture);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 获取远程地址
        String remoteAddress = ctx.channel().remoteAddress().toString();
        log.error(">>>>>>>>>>>>>>>>>>>>>>>>SseHandler: exceptionCaught, remoteAddress={}", remoteAddress, cause);
        // 关闭连接，自动释放相关资源
        ctx.close();
    }

    protected static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
        ctx.write(response);
    }

    protected void sendRetryEvent(ChannelHandlerContext ctx, EventEndpoint endpoint) {
        ByteBuf buffer = ctx.alloc().buffer();
        // 发送 retry 字段
        buffer.writeBytes(("retry: " + endpoint.getRetryMilliseconds() + "\n").getBytes(StandardCharsets.UTF_8));
        // 发送一条空数据消息
        buffer.writeBytes("data: \n\n".getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(new DefaultHttpContent(buffer));
    }

    protected void sendSseEvent(ChannelHandlerContext ctx, EventDTO eventDto) {
        if (eventDto == null) {
            return;
        }
        ByteBuf buffer = ctx.alloc().buffer();
        String message;
        if (StringUtil.isNullOrEmpty(eventDto.getEvent())) {
            message = "data: " + eventDto.getData() + "\n\n";
        } else if (StringUtil.isNullOrEmpty(eventDto.getId())) {
            message = "event: " + eventDto.getEvent() + "\n" +
                    "data: " + eventDto.getData() + "\n\n";
        } else {
            message = "id: " + eventDto.getId() + "\n" +
                    "event: " + eventDto.getEvent() + "\n" +
                    "data: " + eventDto.getData() + "\n\n";
        }
        buffer.writeBytes(message.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(new DefaultHttpContent(buffer));
    }

}