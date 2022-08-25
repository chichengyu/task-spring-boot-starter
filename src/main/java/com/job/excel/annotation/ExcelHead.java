package com.job.excel.annotation;

import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * excel 表头注解
 * 样式优先级: 自定义 > 注解 > 默认
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelHead {
    /**
     * 左右是否居中,默认居中：HorizontalAlignment.CENTER
     */
    HorizontalAlignment align() default HorizontalAlignment.CENTER;

    /**
     * 上下是否居中,默认居中：VerticalAlignment.CENTER
     */
    VerticalAlignment vertical() default VerticalAlignment.CENTER;

    /**
     * 字体大小,默认 10
     */
    short fontSize() default 10;

    /**
     * 是否加粗,默认false
     */
    boolean bold() default false;

    /**
     * 字体名称,如：Arial、微软雅黑...
     */
    String fontName() default "";

    /**
     * 背景颜色,默认正常白色：HSSFColor.HSSFColorPredefined.WHITE.getIndex()=9,或者 IndexedColors.WHITE.getIndex()=9
     */
    IndexedColors backColor() default IndexedColors.WHITE;

    /**
     * 字体颜色,默认正常黑色：HSSFColor.HSSFColorPredefined.BLACK.getIndex()=8,或者 IndexedColors.WHITE.BLACK()=8
     */
    IndexedColors color() default IndexedColors.BLACK;

    /**
     * 是否自动换行,默认 false
     */
    boolean wrap() default false;

    /**
     * 是否使用斜体
     */
    boolean italic() default false;

    /**
     * 是否锁定,不可修改,**注意**:只要设置了锁定,那么导出后的excel,在打开后,很多操作都不能编辑,比如:拖动改变列宽长度也不能进行了
     */
    boolean lock() default false;

    /**
     * 锁定时,设置密码(默认空字符串也可以)
     */
    String lockPassword() default "";
}
