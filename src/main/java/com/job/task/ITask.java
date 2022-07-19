package com.job.task;

import com.job.util.R;

/**
 * @author xiaochi
 */
public interface ITask<T> {

    /**
     * 执行定时任务接口
     */
    R<T> run(String params);
}
