package com.job.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job.util.R;

/**
 * @author xiaochi
 */
public interface ITask<T> {
    ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /**
     * task run excute
     */
    R<T> run(String params);
}
