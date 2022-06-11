## 依赖说明

```xml
<!--spring boot的版本号是1.5.0以后-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!--spring boot的版本号是1.4.0以前-->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-redis</artifactId>
</dependency>
```

- spring boot的版本号是1.4.0~1.5.0之间，添加redis的jar包的时候，添加spring-boot-starter-data-redis或spring-boot-starter-redis是都可以的。
- spring boot的版本号是1.4.0 以前也就是1.3.8 版本以前，添加redis 的 jar包就必须是 spring-boot-starter-redis 的jar包。
- spring boot的版本号是1.5.0以后的，添加redis的jar包就必须是spring-boot-starter-data-redis。

## Lettuce与Jedis的区别

> Redis服务端（6.0之前）是单线程，为啥客户端使用多线程和连接池呢？主要原因：redis的性能瓶颈主要是网络通讯——网络通讯速度比redis处理速度要慢许多。客户端在和服务端在网络通讯的时间里，redis服务端是处于闲暇，无法发挥其处理能力。

- 都是Redis的客户端
- Lettuce
  - 优点
    - 连接是基于Netty实现，可异步调用，线程安全。
    - 适用于分布式缓存。
  - 缺点
    - API更抽象，学习使用成本高
- Jedis
  - 优点
    - API基本与Redis的指令一一对应，使用简单易理解。
  - 缺点
    - 同步阻塞IO，不支持异步。
    - 当多线程使用同一个连接时，非线程安全，所以要使用连接池，为每个jedis实例分配一个连接。即加上JedisPool池子保证线程安全。

### Jedis非线程安全演示

> https://www.jianshu.com/p/5e4a1f92c88f
>
> Jedis类中有RedisInputStream和RedisOutputStream两个属性，而发送命令和获取返回值都是使用这两个成员变量，显然这很容易引发多线程问题。

```java
public class BadConcurrentJedisTest {
    private static final ExecutorService pool = Executors.newFixedThreadPool(20);
    private static final Jedis jedis = new Jedis("127.0.0.1", 6379);

    public static void main(String[] args) {
        for (int i = 0; i < 20; i++) {
            pool.execute(new RedisSet());
        }
    }

    static class RedisSet implements Runnable {
        @Override
        public void run() {
            while (true) {
                jedis.set("hello", "world");
            }
        }
    }
}
```

## 其他

- Redis版本>6.0才提供用户名配置选项。

- **集群选库问题：** 如果redis是集群部署的时候，选择对应的数据库是没用的，因为在redis在进群配置的时候默认使用db0。Redis集群目前无法做数据库选择，默认在 0 数据库。

  - 在单体Redis的情况下可以使用select命令来实现数据库的切换，但在集群环境下，Redis不支持使用select命令来切换数据库，这是因为在集群环境下只有一个db0数据库。集群与单体Redis的区别如下：（https://zhuanlan.zhihu.com/p/349776401）

    1、key批量操作支持有限：例如mget、mset必须在一个slot；
    2、Key事务和Lua支持有限：操作的key必须在一个节点；
    3、key是数据分区的最小粒度：不支持bigkey分区；
    4、不支持多个数据库：集群模式下只有一个db0；
    5、复制只支持一层：不支持树形复制结构。

    应以命名空间的方式理解Redis数据库db，**多个应用程序不应使用同一个Redis的不同库，而应一个应用程序对应一个Redis实例，不同的数据库可用于存储不同环境的数据。**


### 连接池连接数配置

> 这结论不确定，待验证。

**Jedis**

- 调大连接池大小能够提高Jedis的吞吐量，但是不能避免出现超时错误和长时间等待。

- Jedis连接方式最大连接数和最小、最大空闲连接数设置为一样有利于减少上下文切换时间，提升效率。

**Lettuce**

- Lettuce调大连接数大小反而会影响性能，最佳个数= CPU核数+1.
- Lettuce整体稳定性和性能优于Jedis方式。

## 问题

### 问题一

~~如果不是使用Lettuce，而使用Jedis，~~启动的时候可能出现**报错：Unsatisfied dependency expressed through field 'redisTemplate'**（大意是：通过字段'stringRedisTemplate'表示的未满足的依赖关系）。

 **解决方案：**添加连接池依赖~~（不管Lettuce还是Jedis都需要连接池依赖）~~ **待确认**

```xml
<dependency>
	<groupId>org.apache.commons</groupId>
	<artifactId>commons-pool2</artifactId>
</dependency>
```

### 问题二

> https://blog.csdn.net/zhaoheng314/article/details/81564166

```java
// 这样会报错
@Autowired
private RedisTemplate<String, Object> redisTemplate;
// 正确
@Resource
private RedisTemplate<String, Object> redisTemplate;
```

### Redis乱码

> https://blog.csdn.net/weixin_42408447/article/details/120178004
>
> 原因：RedisTemplate默认使用的是JDK序列化器，而它使用的编码是ISO-8859-1



