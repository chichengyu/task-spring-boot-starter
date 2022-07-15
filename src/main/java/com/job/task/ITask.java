package com.job.task;

import com.job.util.R;

/**
 * @author xiaochi
 * @date 2022/6/20 9:59
 * @desc ITask
 */
public interface ITask<T> {

    /**
     * 执行定时任务接口
     * @param params   参数，多参数使用JSON数据
     * @return R
     */
    R<T> run(String params);
}
