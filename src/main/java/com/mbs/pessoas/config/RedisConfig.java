package com.mbs.pessoas.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.mbs.pessoas.service.CepConsumerService;

@Configuration
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.redis.host:redis}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.connect-timeout:10000}")
    private long connectTimeoutMillis;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(redisHost, redisPort);

    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
        .commandTimeout(Duration.ofMillis(connectTimeoutMillis))
        .clientOptions(io.lettuce.core.ClientOptions.builder().autoReconnect(true).build())
        .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(serverConfig, clientConfig);
        // initialize now so any connection issues surface early when running locally (in Docker Compose will retry)
        try {
            factory.afterPropertiesSet();
        } catch (Exception ex) {
            log.warn("Could not initialize LettuceConnectionFactory at startup: {}:{} ({}). Application will continue and attempt connections later.", redisHost, redisPort, ex.getMessage());
        }
        return factory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisMessageListenerContainer container(LettuceConnectionFactory connectionFactory, CepConsumerService consumer) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        try {
            // attempt a short connection check; if it fails, log and still return the container - it will try to reconnect when Redis becomes available
            try (var conn = connectionFactory.getConnection()) {
                if (conn != null) {
                    try {
                        conn.ping();
                    } catch (Exception e) {
                        // ping failed but we don't throw to avoid crashing app; listener registration will still be attempted
                        log.info("Redis ping failed during startup: {}", e.getMessage());
                    }
                }
            }

            container.addMessageListener(consumer, new ChannelTopic("filaCep"));
        } catch (Exception ex) {
            log.warn("Could not register Redis message listener at startup (Redis may be unavailable). Listener will try to subscribe later: {}", ex.getMessage());
        }

        return container;
    }
}