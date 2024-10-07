package com.guzt.starter.sse.config;

import com.guzt.starter.sse.properties.SseProperties;
import com.guzt.starter.sse.service.SseBusinessService;
import com.guzt.starter.sse.service.SseServer;
import com.guzt.starter.sse.service.impl.DefaultSseBusinessServiceImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import javax.annotation.Resource;

/**
 * SSE服务端 核心配置类
 *
 * @author <a href="mailto:gzt19881123@163.com">guzt</a>
 */
@Configuration
@ConditionalOnProperty(prefix = "guzt.sse", value = "enable", havingValue = "true")
@EnableConfigurationProperties({SseProperties.class})
public class SseAutoConfigure implements InitializingBean {

    @Lazy
    @Resource
    private ApplicationContext applicationContext;

    @Bean
    @ConditionalOnMissingBean
    SseServer sseServer() {
        return new SseServer();
    }

    @Bean
    @ConditionalOnMissingBean
    public SseBusinessService defaultSseBusinessService() {
        return new DefaultSseBusinessServiceImpl();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        SseServer sseServer = applicationContext.getBean(SseServer.class);
        sseServer.setSseProperties(applicationContext.getBean(SseProperties.class));
        sseServer.setSseBusinessService(applicationContext.getBean(SseBusinessService.class));
        sseServer.init();
        sseServer.start();
    }

}
