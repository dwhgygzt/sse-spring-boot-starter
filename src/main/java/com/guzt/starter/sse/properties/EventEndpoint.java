package com.guzt.starter.sse.properties;


/**
 * 多个地址订阅的配置文件
 *
 * @author <a href="mailto:gzt19881123@163.com">guzt</a>
 */
@SuppressWarnings("unused")
public class EventEndpoint {

    /**
     * 订阅地址
     */
    private String endpoint = "/events";

    /**
     * event推送事件间隔 单位：毫秒
     */
    private Integer eventPeriodMilliseconds = 5000;

    /**
     * 设置客户端的重试时间间隔 单位：毫秒  SSE机制默认3秒钟
     */
    private Integer retryMilliseconds;

    public Integer getEventPeriodMilliseconds() {
        return eventPeriodMilliseconds;
    }

    public void setEventPeriodMilliseconds(Integer eventPeriodMilliseconds) {
        this.eventPeriodMilliseconds = eventPeriodMilliseconds;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Integer getRetryMilliseconds() {
        return retryMilliseconds;
    }

    public void setRetryMilliseconds(Integer retryMilliseconds) {
        this.retryMilliseconds = retryMilliseconds;
    }
}
