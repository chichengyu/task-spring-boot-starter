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
 * excel 列注解
 * 样式优先级: 自定义 > 注解 > 默认
 * 自定义样式时,一定要设置属性 style = true
 */
@Inherited
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExcelColumn {
    /**
     * 名称
     */
    String name();

    /**
     * 导出时在excel中每个列的宽 单位为字符
     */
    double width() default 8;

    /**
     * 导出时在excel中每个行的高 单位为px,设置height时,自适应autoHeight必须设置为false
     */
    float height() default 20;

    /**
     * 导出时在excel中每个行的高是否自适应,为true则height设置无效,false则height设置生效
     */
    boolean autoHeight() default true;

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
     * 是否使用斜体
     */
    boolean italic() default false;

    /**
     * 是否自动换行,默认 false
     */
    boolean wrap() default false;

    /**
     * 文字后缀,如% 90 变成90%
     */
    String suffix() default "";

    /**
     * 是否锁定,不可修改,**注意**:只要设置了锁定,那么导出后的excel,在打开后,很多操作都不能编辑,比如:拖动改变列宽长度也不能进行了
     */
    boolean lock() default false;

    /**
     * 锁定时,设置密码(默认空字符串也可以)
     */
    String lockPassword() default "";

    /**
     * 日期格式 dateformat = "yyyy-MM-dd HH:mm:ss"
     */
    String dateformat() default "";

    /**
     * 当值为空时,字段的默认值
     */
    String defaultValue() default "";

    /**
     * 内容转表达式 (如: 0=男,1=女,2=未知)
     */
    String converExp() default "";

    /**
     * 导入时，等于默认值 defaultValue 时，则设置为null，默认 false
     */
    boolean readDefaultValue() default false;

    /**
     * 样式是否自定义
     */
    boolean style() default false;
}
