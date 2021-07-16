package org.xzhi.eurekaclient.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * EurekaClientController
 *
 * @author Xzhi
 * @date 2021-07-15 16:26
 */
@RestController
public class EurekaClientController {

    @GetMapping("/test")
    public String test() {
        return "EurekaClientTest";
    }
}