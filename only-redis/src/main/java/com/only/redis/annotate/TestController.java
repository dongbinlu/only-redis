package com.only.redis.annotate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@Slf4j
public class TestController {

    @GetMapping("/lock/{userId}")
    @RedisLock(maxAttempts = 3, timeout = 3)
    public String lock(@PathVariable Integer userId) {

        try {
            log.info("睡眠执行前");
            //Thread.sleep(1 * 1000);
            log.info("睡眠执行后");
        } catch (Exception e) {
            log.info("has some error", e);
        }
        return "成功";
    }

}
