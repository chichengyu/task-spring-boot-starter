package com.job.task;

import com.alibaba.fastjson.JSON;
import com.job.task.pojo.JobTask;
import com.job.task.pojo.JobTaskLog;
import com.job.util.R;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.task.TaskSchedulerBuilder;
import org.springframework.context.ApplicationContext;
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
 * @date 2022/6/11 16:52
 * @desc SchudleManager
 */
@Slf4j
public class TaskManager implements DisposableBean {
    @Setter
    private String prefix = "job_task_";
    @Setter
    private int poolSize = 10;
    @Setter
    private volatile ErrorHandler errorHandler;
    private ApplicationContext applicationContext;
    // 正在运行的定时任务
    @Getter
    private volatile ConcurrentHashMap<Long,ScheduledRealTaskFuture> taskContainer = new ConcurrentHashMap<>();
    @Getter
    private TaskScheduler taskScheduler;
    @Setter
    private Consumer<JobTaskLog> jobTaskLogSave;

    public TaskManager(){}
    public TaskManager(Consumer<JobTaskLog> jobTaskLogSave){
        this.jobTaskLogSave = jobTaskLogSave;
    }

    /**
     * 初始化
     * @param applicationContext
     */
    public void init(ApplicationContext applicationContext){
        ThreadPoolTaskScheduler taskScheduler = new TaskSchedulerBuilder()
                .poolSize(poolSize)
                .threadNamePrefix(prefix)
                .build();
        taskScheduler.setRemoveOnCancelPolicy(true);//是否将取消后的任务从队列删除
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setErrorHandler(errorHandler);
        taskScheduler.initialize();
        this.taskScheduler = taskScheduler;
        this.applicationContext = applicationContext;
    }

    /**
     * 刷新所有定时任务
     */
    public void refresh(List<JobTask> taskPojoList) throws Exception {
        if (!taskPojoList.isEmpty()){
            destroy();
            for (JobTask pojo : taskPojoList){
                addCronTask(pojo);
            }
        }
    }

    /**
     * 创建并启动定时任务
     * @param pojo
     */
    public void addCronTask(JobTask pojo){
        cancel(pojo.getJobId());
        /*CronTask cronTask = new CronTask(() -> {
            System.out.println("执行定时器【"+ pojo.getName() +"】任务:" + LocalDateTime.now().toLocalTime());
        },pojo.getCron());*/
        CronTask cronTask = new CronTask(() -> execute(pojo),pojo.getCronExpression());
        ScheduledRealTaskFuture realTaskFuture = new ScheduledRealTaskFuture();
        realTaskFuture.future = taskScheduler.schedule(cronTask.getRunnable(), cronTask.getTrigger());
        taskContainer.put(pojo.getJobId(),realTaskFuture);
    }

    /**
     * 更新定时任务
     */
    public void updateCronTask(JobTask pojo) throws Exception {
        cancel(pojo.getJobId());
        addCronTask(pojo);
    }

    /**
     * 立即执行定时任务
     * @param jobTask
     */
    public void runNow(JobTask jobTask){
        if (jobTask != null){
            execute(jobTask);
        }
    }

    /**
     * 取消定时任务
     * @param jobId
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
     * 销毁的时候停止定时任务
     */
    @Override
    public void destroy(){
        for (ScheduledRealTaskFuture future : taskContainer.values()){
            future.cancel();
        }
        taskContainer.clear();// 清空容器
    }

    /**
     * 取消任务的 ScheduledFuture
     */
    private static class ScheduledRealTaskFuture{
        public volatile ScheduledFuture<?> future;
        /**
         * 取消定时任务
         */
        public void cancel() {
            ScheduledFuture<?> future = this.future;
            if (future != null) {
                future.cancel(true);
            }
        }
    }

    /**
     * 定时任务执行且日志入库
     * @param jobTask
     */
    private void execute(JobTask jobTask){
        //数据库保存执行记录
        //ScheduleJobLog jobLog = ScheduleJobLog.builder().jobId(scheduleJob.getJobId()).beanName(scheduleJob.getBeanName()).params(scheduleJob.getParams()).createTime(new Date()).build();
        JobTaskLog jobLog = new JobTaskLog();
        jobLog.setJobId(jobTask.getJobId());
        jobLog.setBeanName(jobTask.getBeanName());
        jobLog.setParams(jobTask.getParams());
        jobLog.setCreateTime(new Date());
        // 任务开始执行时间
        long startTime = System.currentTimeMillis();
        try {
            log.info("定时任务[{}]准备执行", jobTask.getJobId());
            Object target = this.applicationContext.getBean(jobTask.getBeanName());
            Method method = target.getClass().getDeclaredMethod("run", String.class);
            R<?> result = (R<?>)method.invoke(target, JSON.toJSONString(jobTask));
            // 任务执行时长
            long times = System.currentTimeMillis() - startTime;
            jobLog.setTimes((int) times);
            jobLog.setStatus(0);
            if (null != result) {
                jobLog.setStatus(result.getCode());
                jobLog.setMessage(result.getMsg());
            }
            log.info("定时任务[{}]执行完毕，总共耗时：{}毫秒", jobTask.getJobId(),times);
        }catch (Exception e){
            log.error("定时任务[{}]执行失败，异常信息：{}", jobTask.getJobId(),e);
            // 任务执行时长
            long times = System.currentTimeMillis() - startTime;
            // 任务状态    0：成功  1：失败  记录数据库
            jobLog.setTimes((int)times);
            jobLog.setStatus(1);
            jobLog.setError(e.toString());
        }finally {
            // 最终记录到数据库
            if (jobTaskLogSave != null){
                jobTaskLogSave.accept(jobLog);
            }
        }
    }
}
