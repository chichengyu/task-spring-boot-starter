package com.job.task;

import com.job.util.R;

/**
 * @author xiaochi
 */
public interface ITask<T> {

    /**
     * task run excute
     */
    R<T> run(String params);
}
