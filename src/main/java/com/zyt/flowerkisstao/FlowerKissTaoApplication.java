package com.zyt.flowerkisstao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @EnableScheduling} 供养护模块的 {@code CareScheduler} 使用。
 * 方案架构章节："Spring Task 定时生成养护任务和站内提醒。"
 */
@SpringBootApplication
@EnableScheduling
public class FlowerKissTaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowerKissTaoApplication.class, args);
    }

}
