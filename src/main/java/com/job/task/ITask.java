package com.job.task;

import com.job.util.R;

/** task base interface
 * @author xiaochi
 */
public interface ITask<T> {

    /**
     * task run excute
     */
    R<T> run(String params);
}
