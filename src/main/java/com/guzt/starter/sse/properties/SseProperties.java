package com.guzt.starter.sse.properties;


import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * SSE 配置文件
 *
 * @author <a href="mailto:gzt19881123@163.com">guzt</a>
 */
@ConfigurationProperties(prefix = "guzt.sse")
public class SseProperties {

    /**
     * 是否启用 true 启用  false 禁用
     */
    private boolean enable;

    /**
     * 服务绑定的端口，SSE需要单独的端口
     */
    private Integer port = 8849;

    /**
     * 最大内容长度
     */
    private Integer maxContentLength = 1024 * 1024;

    /**
     * 最大TCP连接数
     */
    private Integer maxTcpConnections = 512;

    /**
     * URI配置
     */
    List<EventEndpoint> eventEndpoints = new ArrayList<>(4);

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(Integer maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public Integer getMaxTcpConnections() {
        return maxTcpConnections;
    }

    public void setMaxTcpConnections(Integer maxTcpConnections) {
        this.maxTcpConnections = maxTcpConnections;
    }

    public List<EventEndpoint> getEventEndpoints() {
        return eventEndpoints;
    }

    public void setEventEndpoints(List<EventEndpoint> eventEndpoints) {
        this.eventEndpoints = eventEndpoints;
    }
}
