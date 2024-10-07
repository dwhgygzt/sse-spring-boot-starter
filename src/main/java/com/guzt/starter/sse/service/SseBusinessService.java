package com.guzt.starter.sse.service;

import com.guzt.starter.sse.pojo.dto.EventDTO;

import java.util.List;
import java.util.Map;

/**
 * SSE 业务方法处理器
 *
 * @author <a href="mailto:gzt19881123@163.com">guzt</a>
 */
@SuppressWarnings("unused")
public interface SseBusinessService {

    /**
     * 验证连接是否合法
     *
     * @param uri        get地址不包括参数部分
     * @param headers    请求头
     * @param parameters get请求参数
     * @return true 验证通过，false 验证失败
     */
    boolean connectAuth(String uri, Map<String, String> headers, Map<String, List<String>> parameters);

    /**
     * 生成SSE标准的事件
     *
     * @param uri        get地址不包括参数部分
     * @param headers    请求头
     * @param parameters get请求参数
     * @return 事件
     */
    EventDTO generateEvent(String uri, Map<String, String> headers, Map<String, List<String>> parameters);
}
