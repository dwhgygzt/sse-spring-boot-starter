package com.guzt.starter.sse.service.impl;

import com.guzt.starter.sse.pojo.dto.EventDTO;
import com.guzt.starter.sse.service.SseBusinessService;

import java.util.List;
import java.util.Map;

/**
 * SSE 业务方法处理器-默认实现
 *
 * @author <a href="mailto:gzt19881123@163.com">guzt</a>
 */
@SuppressWarnings("unused")
public class DefaultSseBusinessServiceImpl implements SseBusinessService {
    @Override
    public boolean connectAuth(String uri, Map<String, String> headers, Map<String, List<String>> parameters) {
        return true;
    }

    @Override
    public EventDTO generateEvent(String uri, Map<String, String> headers, Map<String, List<String>> parameters) {
        return new EventDTO(System.currentTimeMillis() + "");
    }
}
