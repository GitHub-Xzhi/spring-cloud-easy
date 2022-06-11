package org.xzhi.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.xzhi.standalone.config.RedisUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * RedisStandAloneApp
 *
 * @author Xzhi
 * @date 2022-02-09 18:13
 */
@SpringBootApplication
@Slf4j
@Import(RedisUtil.class)
public class RedisStandAloneApp {
    public static void main(String[] args) {
        SpringApplication.run(RedisStandAloneApp.class, args);
    }
}