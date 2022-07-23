package com.job.task;

import com.job.task.pojo.JobTask;
import com.job.task.pojo.JobTaskLog;
import com.job.util.JsonUtil;
import com.job.util.R;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Properties;
import java.util.function.Consumer;

/**
 * @author xiaochi
 */
public class TaskQuartzManager extends ApplicationObjectSupport {
    private static Logger LOGGER = LoggerFactory.getLogger(TaskQuartzManager.class);
    private String jobNamePrefix = "qrtz_task_";// prefix
    public static String jobParamKey = "JOB_PARAM_KEY";// param key
    private static ApplicationContext applicationContext;
    private SchedulerFactoryBean schedulerFactoryBean;
    public TaskQuartzManager(){}
    public TaskQuartzManager(Consumer<JobTaskLog> jobTaskLogSave){
        TaskQuartzManager.QuartzJob.jobTaskLogSave = jobTaskLogSave;
    }

    @Override
    protected void initApplicationContext(ApplicationContext context) throws BeansException {
        super.initApplicationContext(context);
        if (TaskQuartzManager.applicationContext == null){
            TaskQuartzManager.applicationContext = context;
        }
    }

    /**
     * 默认配置
     */
    public static SchedulerFactoryBean getSchedulerFactoryBean(){
        SchedulerFactoryBean factory = new SchedulerFactoryBean();
        //quartz参数
        Properties prop = new Properties();
        prop.put("org.quartz.scheduler.instanceName", "TaskScheduler");
        prop.put("org.quartz.scheduler.instanceId", "AUTO");
        //线程池配置
        prop.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        prop.put("org.quartz.threadPool.threadCount", "20");
        prop.put("org.quartz.threadPool.threadPriority", "5");
        //JobStore配置
        //prop.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        //prop.put("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        //集群配置
        //prop.put("org.quartz.jobStore.isClustered", "false");// 集群时一定要设置为 true
        //prop.put("org.quartz.jobStore.clusterCheckinInterval", "15000");
        //prop.put("org.quartz.jobStore.maxMisfiresToHandleAtATime", "1");

        //prop.put("org.quartz.jobStore.misfireThreshold", "12000");
        //prop.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
        //prop.put("org.quartz.jobStore.selectWithLockSQL", "SELECT * FROM {0}LOCKS UPDLOCK WHERE LOCK_NAME = ?");

        //PostgreSQL数据库，需要打开此注释
        //prop.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");

        factory.setQuartzProperties(prop);
        factory.setSchedulerName("TaskScheduler");
        //延时启动
        factory.setStartupDelay(30);
        factory.setApplicationContextSchedulerContextKey("applicationContextKey");
        //可选，QuartzScheduler 启动时更新己存在的Job，这样就不用每次修改targetObject后删除qrtz_job_details表对应记录了
        factory.setOverwriteExistingJobs(true);
        //设置自动启动，默认为true
        factory.setAutoStartup(true);
        return factory;
    }

    /**
     * init.
     */
    public void init(){
        LOGGER.info("TaskQuartzManager init finished.");
        if (this.schedulerFactoryBean == null){
            this.schedulerFactoryBean = TaskQuartzManager.getSchedulerFactoryBean();
        }
    }

    /**
     * 获取调度器
     */
    public Scheduler getScheduler(){
        return schedulerFactoryBean.getScheduler();
    }

    /**
     * 获取 jobkey
     */
    public JobKey getJobKey(Long jobId){
        return JobKey.jobKey(jobNamePrefix + jobId);
    }

    /**
     * 获取 triggerKey
     */
    public TriggerKey getTriggerKey(Long jobId){
        return TriggerKey.triggerKey(jobNamePrefix + jobId);
    }

    /**
     * 获取表达式触发器
     */
    public CronTrigger getCronTrigger(Scheduler scheduler,Long jobId){
        try {
            return (CronTrigger) scheduler.getTrigger(getTriggerKey(jobId));
        } catch (SchedulerException e) {
            throw new RuntimeException("获取定时任务CronTrigger出现异常", e);
        }
    }

    /**
     * 创建任务
     * jobTask 任务实体类（对应自定义的数据库任务表）
     * Date 返回创建任务成功后执行时间
     */
    public Date addCronTask(JobTask jobTask){
        try {
            JobKey jobKey = getJobKey(jobTask.getJobId());
            Scheduler scheduler = getScheduler();
            JobDetail jobDetail = JobBuilder.newJob(QuartzJob.class).withIdentity(jobKey).build();
            // 不触发立即执行，等待下次Cron触发频率到达时刻开始按照Cron频率依次执行  withMisfireHandlingInstructionDoNothing
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(jobTask.getCron()).withMisfireHandlingInstructionDoNothing();
            CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobTask.getJobId())).startNow().withSchedule(cronScheduleBuilder).build();
            jobDetail.getJobDataMap().put(TaskQuartzManager.jobParamKey,jobTask);
            if (scheduler.checkExists(jobKey)){
                scheduler.deleteJob(jobKey);
            }
            Date startTime = scheduler.scheduleJob(jobDetail,trigger);
            if (jobTask.getStatus() == 1){// 0表示继续，1表示暂停
                pauseJob(jobTask.getJobId());
            }
            return startTime;
        } catch (SchedulerException e) {
            throw new RuntimeException("创建定时任务失败",e);
        }
    }

    /**
     * 更新定时任务
     * jobTask 任务实体类（对应自定义的数据库任务表）
     * Date 返回更新任务成功后执行时间
     */
    public Date updateCronTask(JobTask jobTask){
        try {
            TriggerKey triggerKey = getTriggerKey(jobTask.getJobId());
            Scheduler scheduler = getScheduler();
            // 2.获取 cron 表达式调度构建器
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(jobTask.getCron()).withMisfireHandlingInstructionDoNothing();
            CronTrigger trigger = getCronTrigger(scheduler, jobTask.getJobId());
            // 按新的cron表达式重新构建trigger
            trigger = trigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(scheduleBuilder).build();
            trigger.getJobDataMap().put(TaskQuartzManager.jobParamKey,jobTask);
            Date updateTime = scheduler.rescheduleJob(triggerKey,trigger);
            if (jobTask.getStatus() == 1){// 0表示继续，1表示暂停
                pauseJob(jobTask.getJobId());
            }
            return updateTime;
        } catch (SchedulerException e) {
            throw new RuntimeException("更新定时任务失败",e);
        }
    }

    /**
     * 立即执行（让定时任务立即执行，**注意：暂停状态下的定时任务，如果立即执行，只会执行一次，相当于一次性执行）
     * jobTask 任务实体类（对应自定义的数据库任务表）
     */
    public void runNow(JobTask jobTask){
        try {
            // 参数
            JobDataMap dataMap = new JobDataMap();
            dataMap.put(TaskQuartzManager.jobParamKey,jobTask);

            getScheduler().triggerJob(getJobKey(jobTask.getJobId()),dataMap);
        } catch (SchedulerException e) {
            throw new RuntimeException("立即执行定时任务失败",e);
        }
    }

    /**
     * 暂停任务
     * jobId 任务jobId
     */
    public void pauseJob(Long jobId){
        try {
            getScheduler().pauseJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            throw new RuntimeException("暂停定时任务失败",e);
        }
    }

    /**
     * 恢复任务
     * jobId 任务jobId
     */
    public void resumeJob(Long jobId){
        try {
            getScheduler().resumeJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            throw new RuntimeException("恢复定时任务失败",e);
        }
    }

    /**
     * 验证定时任务是否存在
     * jobId 任务id
     */
    public boolean check(Long jobId){
        try {
            return getScheduler().checkExists(getJobKey(jobId));
        } catch (SchedulerException e) {
            throw new RuntimeException("验证定时任务是否存在失败",e);
        }
    }

    /**
     * 删除定时任务
     * jobId 定时任务id
     */
    public void deleteTask(Long jobId){
        Scheduler scheduler = getScheduler();
        try {
            scheduler.pauseTrigger(getTriggerKey(jobId));
            scheduler.unscheduleJob(getTriggerKey(jobId));
            scheduler.deleteJob(getJobKey(jobId));
        } catch (SchedulerException e) {
            throw new RuntimeException("删除定时任务失败",e);
        }
    }

    @DisallowConcurrentExecution//上次任务没有执行完，下次任务推迟执行
    public static class QuartzJob extends QuartzJobBean {
        private static Logger LOGGER = LoggerFactory.getLogger(QuartzJob.class);
        private static Consumer<JobTaskLog> jobTaskLogSave;

        @Override
        protected void executeInternal(JobExecutionContext jobExecutionContext){
            JobTask jobTask = (JobTask) jobExecutionContext.getMergedJobDataMap().get(TaskQuartzManager.jobParamKey);
            //数据库保存执行记录
            JobTaskLog jobTaskLog = new JobTaskLog();
            jobTaskLog.setJobId(jobTask.getJobId());
            jobTaskLog.setBeanName(jobTask.getBeanName());
            jobTaskLog.setParams(jobTask.getParams());
            jobTaskLog.setCreateTime(new Date());
            long startTime = System.currentTimeMillis();
            try {
                //执行任务
                LOGGER.debug("任务[{}]准备执行",jobTask.getJobId());
                Object target = applicationContext.getBean(jobTask.getBeanName());
                Method method = target.getClass().getDeclaredMethod("run", String.class);
                R<?> result = (R<?>)method.invoke(target, JsonUtil.toJson(jobTask));
                // 任务执行时长
                long times = System.currentTimeMillis() - startTime;
                //任务状态    0：成功    1：失败
                jobTaskLog.setTimes((int) times);
                jobTaskLog.setStatus(0);
                if (null != result) {
                    jobTaskLog.setStatus(result.getCode());
                    jobTaskLog.setMessage(result.getMsg());
                }
                LOGGER.debug("任务[{}]执行完毕,耗时:{}毫秒", jobTask.getJobId(),times);
            } catch (Exception e) {
                LOGGER.error("任务[{}]执行失败,异常信息:{}", jobTask.getJobId(),e);
                // 任务执行时长
                long times = System.currentTimeMillis() - startTime;
                // 任务状态    0：成功  1：失败  记录数据库
                jobTaskLog.setTimes((int)times);
                jobTaskLog.setStatus(1);
                jobTaskLog.setError(e.toString());
            }finally {
                // final save db
                if (jobTaskLogSave != null){
                    jobTaskLogSave.accept(jobTaskLog);
                }
            }
        }
    }

    public void setJobNamePrefix(String jobNamePrefix) {
        this.jobNamePrefix = jobNamePrefix;
    }

    public void setJobParamKey(String jobParamKey) {
        TaskQuartzManager.jobParamKey = jobParamKey;
    }

    public void setSchedulerFactoryBean(SchedulerFactoryBean schedulerFactoryBean) {
        this.schedulerFactoryBean = schedulerFactoryBean;
    }

    public void setJobTaskLogSave(Consumer<JobTaskLog> jobTaskLogSave) {
        TaskQuartzManager.QuartzJob.jobTaskLogSave = jobTaskLogSave;
    }

    public String getJobNamePrefix() {
        return this.jobNamePrefix;
    }

    public static String getJobParamKey() {
        return TaskQuartzManager.jobParamKey;
    }

    public Consumer<JobTaskLog> getJobTaskLogSave() {
        return TaskQuartzManager.QuartzJob.jobTaskLogSave;
    }

    public static ApplicationContext getContext() {
        return applicationContext;
    }
}
