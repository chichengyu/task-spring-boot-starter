package com.job.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * excel 注解
 */
@Inherited
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Excel {
    /**
     * 名称
     */
    String name();

    /**
     * 导出时在excel中每个列的宽 单位为字符
     */
    double width() default 8;

    /**
     * 文字后缀,如% 90 变成90%
     */
    String suffix() default "";

    /**
     * 日期格式 dateformat = "yyyy-MM-dd HH:mm:ss"
     */
    String dateformat() default "";

    /**
     * 当值为空时,字段的默认值
     */
    String defaultValue() default "";

    /**
     * 读取内容转表达式 (如: 0=男,1=女,2=未知)
     */
    String readConverterExp() default "";

    /**
     * 导入时，等于默认值 defaultValue 时，则设置为null，默认 false
     */
    boolean readCoverDefaultValue() default false;
}
