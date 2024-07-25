package com.job.task;

import com.job.task.pojo.JobTask;
import com.job.task.pojo.JobTaskLog;
import com.job.util.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.CronTask;
import org.springframework.util.ErrorHandler;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.function.Consumer;

/**
 * @author xiaochi
 */
public class TaskManager extends ApplicationObjectSupport implements DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskManager.class);
    private String prefix = "job_task_";
    private int poolSize = 10;
    private volatile ErrorHandler errorHandler;
    private static ApplicationContext applicationContext;
    private volatile ConcurrentHashMap<Long,ScheduledRealTaskFuture> taskContainer = new ConcurrentHashMap<>();
    private TaskScheduler taskScheduler;
    private Consumer<JobTaskLog> jobTaskLogSave;
    public TaskManager(){}
    public TaskManager(Consumer<JobTaskLog> jobTaskLogSave){
        this.jobTaskLogSave = jobTaskLogSave;
    }

    @Override
    protected void initApplicationContext(ApplicationContext context) throws BeansException {
        super.initApplicationContext(context);
        if (TaskManager.applicationContext == null){
            TaskManager.applicationContext = context;
        }
    }

    /**
     * init.
     */
    public void init(){
        ThreadPoolTaskScheduler taskScheduler = new TaskSchedulerBuilder().poolSize(poolSize).threadNamePrefix(prefix).build();
        taskScheduler.setRemoveOnCancelPolicy(true);//是否将取消后的任务从队列删除
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setErrorHandler(errorHandler);
        taskScheduler.initialize();
        this.taskScheduler = taskScheduler;
        LOGGER.info("TaskManager init finished.");
    }

    /**
     * refresh all task.
     */
    public void refresh(List<JobTask> taskPojoList){
        if (!taskPojoList.isEmpty()){
            destroy();
            for (JobTask pojo : taskPojoList){
                addCronTask(pojo);
            }
        }
    }

    /**
     * create and start task.
     */
    public void addCronTask(JobTask pojo){
        cancel(pojo.getJobId());
        CronTask cronTask = new CronTask(() -> execute(pojo),pojo.getCron());
        ScheduledRealTaskFuture realTaskFuture = new ScheduledRealTaskFuture();
        realTaskFuture.future = taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
        taskContainer.put(pojo.getJobId(),realTaskFuture);
    }

    /**
     * update task.
     */
    public void updateCronTask(JobTask pojo){
        cancel(pojo.getJobId());
        addCronTask(pojo);
    }

    /**
     * run now task.
     */
    public void runNow(JobTask jobTask){
        if (jobTask != null){
            execute(jobTask);
        }
    }

    /**
     * cancel task.
     */
    public void cancel(Long jobId){
        if (taskContainer.containsKey(jobId)){
            ScheduledRealTaskFuture taskFuture = taskContainer.get(jobId);
            if (taskFuture != null){
                taskFuture.cancel();
            }
            taskContainer.remove(jobId);
        }
    }

    /**
     * destroy and cancel task.
     */
    @Override
    public void destroy(){
        for (ScheduledRealTaskFuture future : taskContainer.values()){
            future.cancel();
        }
        taskContainer.clear();
    }

    /**
     * cancel task ScheduledFuture.
     */
    public static class ScheduledRealTaskFuture{
        public volatile ScheduledFuture<?> future;
        /**
         * cancel task.
         */
        public void cancel() {
            ScheduledFuture<?> future = this.future;
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    /**
     * task db save
     */
    private void execute(JobTask jobTask){
        //ScheduleJobLog jobLog = ScheduleJobLog.builder().jobId(scheduleJob.getJobId()).beanName(scheduleJob.getBeanName()).params(scheduleJob.getParams()).createTime(new Date()).build();
        JobTaskLog jobLog = new JobTaskLog();
        jobLog.setJobId(jobTask.getJobId());
        jobLog.setBeanName(jobTask.getBeanName());
        jobLog.setParams(jobTask.getParams());
        jobLog.setCreateTime(new Date());
        long startTime = System.currentTimeMillis();
        try {
            Object target = applicationContext.getBean(jobTask.getBeanName());
            Method method = target.getClass().getDeclaredMethod("run", String.class);
            R<?> result = (R<?>)method.invoke(target, ITask.OBJECT_MAPPER.writeValueAsString(jobTask));
            // 任务执行时长
            long times = System.currentTimeMillis() - startTime;
            jobLog.setTimes((int) times);
            jobLog.setStatus(0);
            if (null != result) {
                jobLog.setStatus(result.getCode());
                jobLog.setMessage(result.getMsg());
            }
            LOGGER.debug("任务[{}]执行完毕,耗时:{}毫秒", jobTask.getJobId(),times);
        }catch (Exception e){
            LOGGER.error("任务[{}]执行失败,异常信息:{}", jobTask.getJobId(),e);
            String message = e.getMessage();
            if (message==null || "".equals(message)){
                message = e.getCause().getMessage();
            }
            // 任务执行时长
            long times = System.currentTimeMillis() - startTime;
            // 任务状态    0：成功  1：失败  记录数据库
            jobLog.setTimes((int)times);
            jobLog.setStatus(1);
            jobLog.setError(message);
        }finally {
            // final save db
            if (jobTaskLogSave != null){
                jobTaskLogSave.accept(jobLog);
            }
        }
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setJobTaskLogSave(Consumer<JobTaskLog> jobTaskLogSave) {
        this.jobTaskLogSave = jobTaskLogSave;
    }

    public ConcurrentHashMap<Long, ScheduledRealTaskFuture> getTaskContainer() {
        return taskContainer;
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public static ApplicationContext getContext() {
        return applicationContext;
    }
}
