package com.job.task.pojo;

import java.io.Serializable;
import java.util.Date;

/**
 * @author xiaochi
 */
public class JobTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * task id
     */
    private Long jobId;
    /**
     * container bean name
     */
    private String beanName;
    /**
     * params
     */
    private String params;
    /**
     * cron
     */
    private String cron;
    /**
     * task status . 0 open 1 disabled
     */
    private Integer status;
    /**
     * remarks
     */
    private String remark;
    /**
     * create time
     */
    private Date createTime;

    public JobTask() {
    }

    public JobTask(Long jobId, String beanName, String params, String cron, Integer status, String remark, Date createTime) {
        this.jobId = jobId;
        this.beanName = beanName;
        this.params = params;
        this.cron = cron;
        this.status = status;
        this.remark = remark;
        this.createTime = createTime;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
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

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "JobTask{" +
                "jobId=" + jobId +
                ", beanName='" + beanName + '\'' +
                ", params='" + params + '\'' +
                ", cron='" + cron + '\'' +
                ", status=" + status +
                ", remark='" + remark + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
