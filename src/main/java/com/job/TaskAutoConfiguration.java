package com.job;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * @author xiaochi
 * @date 2022/7/15 11:21
 * @desc TaskAutoConfiguration
 */
@Configuration
@ConditionalOnProperty(name = "task.enabled",havingValue = "false")
public class TaskAutoConfiguration {

    /*@Bean
    public ScheduleManager scheduleManager(){
        ScheduleManager scheduleManager = new ScheduleManager();
        scheduleManager.init();
        return scheduleManager;
    }*/
}
