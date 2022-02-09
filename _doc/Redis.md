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

