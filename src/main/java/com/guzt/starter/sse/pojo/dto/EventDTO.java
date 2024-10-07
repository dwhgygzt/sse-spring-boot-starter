package com.guzt.starter.sse.pojo.dto;

/**
 * SSE 推送的消息字段
 *
 * @author <a href="mailto:gzt19881123@163.com">guzt</a>
 */
@SuppressWarnings("unused")
public class EventDTO {

    public EventDTO(String data) {
        this.data = data;
    }

    public EventDTO(String event, String data) {
        this.event = event;
        this.data = data;
    }

    public EventDTO(String id, String event, String data) {
        this.id = id;
        this.event = event;
        this.data = data;
    }

    /**
     * 消息id 可为空
     */
    private String id;

    /**
     * 消息类型 为空默认表示为 message
     */
    private String event;

    /**
     * 消息内容
     */
    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
