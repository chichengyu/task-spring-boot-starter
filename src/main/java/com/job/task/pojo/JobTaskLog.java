package com.job.task.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xiaochi
 * @date 2022/6/11 16:44
 * @desc ScheduleJobLog
 */
public class JobTaskLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * task log id
     */
    private Long logId;
    /**
     * task id
     */
    private Long jobId;
    /**
     * spring bean name
     */
    private String beanName;
    /**
     * params
     */
    private String params;
    /**
     * task status . 0 success 1 fail
     */
    private Integer status;
    /**
     * success message
     */
    private String message;
    /**
     * faill message
     */
    private String error;
    /**
     * total time . ms
     */
    private Integer times;
    /**
     * create time
     */
    private Date createTime;

    public JobTaskLog() {
    }

    public JobTaskLog(Long logId, Long jobId, String beanName, String params, Integer status, String message, String error, Integer times, Date createTime) {
        this.logId = logId;
        this.jobId = jobId;
        this.beanName = beanName;
        this.params = params;
        this.status = status;
        this.message = message;
        this.error = error;
        this.times = times;
        this.createTime = createTime;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "JobTaskLog{" +
                "logId=" + logId +
                ", jobId=" + jobId +
                ", beanName='" + beanName + '\'' +
                ", params='" + params + '\'' +
                ", status=" + status +
                ", message='" + message + '\'' +
                ", error='" + error + '\'' +
                ", times=" + times +
                ", createTime=" + createTime +
                '}';
    }
}
