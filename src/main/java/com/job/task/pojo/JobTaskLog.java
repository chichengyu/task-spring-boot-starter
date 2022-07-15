package com.job.task.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xiaochi
 * @date 2022/6/11 16:44
 * @desc ScheduleJobLog
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobTaskLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 日志主建id
     */
    private Long logId;
    /**
     * 任务id
     */
    private Long jobId;
    /**
     * spring bean名称
     */
    private String beanName;
    /**
     * 参数
     */
    private String params;
    /**
     * 任务状态0：成功1：失败
     */
    private Integer status;
    /**
     * 正常信息
     */
    private String message;
    /**
     * 失败信息
     */
    private String error;
    /**
     * 耗时(单位：毫秒)
     */
    private Integer times;
    /**
     * 创建时间
     */
    private Date createTime;
}
