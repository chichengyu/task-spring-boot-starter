package com.job.excel;

/**
 * Excel数据格式处理适配器
 */
public interface ExcelHandlerAdapter {
    /**
     * 格式化
     * @param value 单元格数据值
     * @return 处理后的值
     */
    Object format(Object value);
}
