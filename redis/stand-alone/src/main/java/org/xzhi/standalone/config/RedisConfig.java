package org.xzhi.standalone.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.PostConstruct;

/**
 * Redis配置
 *
 * @author Xzhi
 * @date 2022-02-16 11:23
 */
// @Configuration
// @EnableCaching
public class RedisConfig {

    public static final String REDIS_CLUSTER_ENABLE = "spring.redis.cluster-enable";

    /**
     * 是否启动redis集群
     */
    @Value("${spring.redis.cluster-enable:false}")
    private boolean clusterEnable;

    @Autowired
    private RedisProperties redisProperties;

    private RedisProperties.Cluster cluster;

    private RedisProperties.Pool pool;

    @PostConstruct
    public void init() {
        cluster = redisProperties.getCluster();
        pool = redisProperties.getLettuce().getPool();
    }

    @Bean
    public StringRedisTemplate strRedisTemplate(RedisConnectionFactory rcf, RedisSerializer jacksonSeial) {
        StringRedisTemplate template = new StringRedisTemplate();
        // 配置连接工厂
        template.setConnectionFactory(rcf);

        template.setValueSerializer(jacksonSeial);
        template.setHashValueSerializer(jacksonSeial);

        // 使用String方式来序列化和反序列化redis的key
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * lettucel 连接池
     */
    @Bean
    public LettucePoolingClientConfiguration lettucePool() {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        // 连接池中的最大空闲连接
        poolConfig.setMaxIdle(pool.getMaxIdle());
        // 连接池中的最小空闲连接
        poolConfig.setMinIdle(pool.getMinIdle());
        // 连接池最大连接数（使用负值表示没有限制）
        poolConfig.setMaxTotal(pool.getMaxActive());
        // 连接池最大阻塞等待时间（使用负值表示没有限制）
        poolConfig.setMaxWaitMillis(pool.getMaxWait().toMillis());

        // 在获取Jedis连接时，自动检验连接是否可用
        poolConfig.setTestOnBorrow(true);
        // 在将连接放回池中前，自动检验连接是否有效
        poolConfig.setTestOnReturn(true);
        // 自动测试池中的空闲连接是否都是可用连接
        poolConfig.setTestWhileIdle(true);
        // 连接耗尽时是否阻塞, false报异常,ture阻塞直到超时,默认true
        poolConfig.setBlockWhenExhausted(false);
        // 表示idle object evitor两次扫描之间要sleep的毫秒数
        poolConfig.setTimeBetweenEvictionRunsMillis(30000);
        // 表示idle object evitor每次扫描的最多的对象数
        poolConfig.setNumTestsPerEvictionRun(10);
        // 表示一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；
        // 这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
        poolConfig.setMinEvictableIdleTimeMillis(60000);
        return LettucePoolingClientConfiguration.builder()
                .poolConfig(poolConfig)
                // 设置连接超时
                .commandTimeout(redisProperties.getTimeout())
                .build();
    }

    /**
     * redis连接工厂
     */
    @Bean
    public RedisConnectionFactory rcf(
            @Autowired(required = false) RedisStandaloneConfiguration redisStandalone,
            @Autowired(required = false) RedisClusterConfiguration redisCluster,
            LettucePoolingClientConfiguration lettucePool) {
        if (clusterEnable) {
            return new LettuceConnectionFactory(redisCluster, lettucePool);
        }
        return new LettuceConnectionFactory(redisStandalone, lettucePool);
    }

    /**
     * redis单机
     */
    @Bean
    @ConditionalOnProperty(name = REDIS_CLUSTER_ENABLE, havingValue = "false", matchIfMissing = true)
    public RedisStandaloneConfiguration redisStandalone() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(
                redisProperties.getHost(), redisProperties.getPort());
        redisStandaloneConfiguration.setDatabase(redisProperties.getDatabase());
        return redisStandaloneConfiguration;
    }

    /**
     * redis集群
     */
    @Bean
    @ConditionalOnProperty(name = REDIS_CLUSTER_ENABLE, havingValue = "true")
    public RedisClusterConfiguration redisCluster() {
        RedisClusterConfiguration redisClusterConfig = new RedisClusterConfiguration(cluster.getNodes());
        redisClusterConfig.setPassword(redisProperties.getPassword());
        // 在整个集群中执行命令时要遵循的最大重定向数
        redisClusterConfig.setMaxRedirects(cluster.getMaxRedirects());
        return redisClusterConfig;
    }

    /**
     * 使用jackson方式来序列化和反序列化redis的value
     */
    @Bean
    public RedisSerializer jacksonSeial() {
        Jackson2JsonRedisSerializer<Object> jacksonSeial = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

        om.registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
        om.findAndRegisterModules();

        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        jacksonSeial.setObjectMapper(om);
        return jacksonSeial;
    }
}