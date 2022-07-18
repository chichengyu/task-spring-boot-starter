package com.job.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author xiaochi
 * @date 2022/7/18 15:34
 * @desc JsonUtil
 */
public class JsonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtil.class);

    /**
     * 实体转json
     * @param o
     * @return
     */
    public static String toJson(Object o){
        StringBuilder sb = new StringBuilder();
        Field[] fields = getAllFields(o.getClass());
        sb.append("{");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0,len = fields.length; i < len; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            String key = field.getName();
            if (key.contains("serialVersionUID")){
                continue;
            }
            sb.append("\"").append(key).append("\"").append(":");
            Object val = getFieldValueByName(key, o);
            if (val == null || "".equals(val)){
                sb.append(val);
            }else {
                Class<?> fieldType = field.getType();
                if (String.class == fieldType){
                    sb.append("\"").append(val).append("\"");
                }else if (Date.class == fieldType){
                    sb.append("\"").append(dateFormat.format(val)).append("\"");
                }else {
                    sb.append(val);
                }
            }
            if ((i+1) != len){
                sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 获取本类及其父类的属性的方法
     * @param clazz 当前类对象
     * @return 字段数组
     */
    private static Field[] getAllFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        while (clazz != null){
            fieldList.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
            clazz = clazz.getSuperclass();
        }
        Field[] fields = new Field[fieldList.size()];
        return fieldList.toArray(fields);
    }

    /**
     * 根据属性名获取属性值
     * @param fieldName 属性名称
     * @param o 对象
     * @return
     */
    private static Object getFieldValueByName(String fieldName, Object o) {
        try {
            String firstLetter = fieldName.substring(0, 1).toUpperCase();
            String getter = "get" + firstLetter + fieldName.substring(1);
            Method method = o.getClass().getMethod(getter, new Class[] {});
            Object value = method.invoke(o, new Object[] {});
            return value;
        } catch (Exception e) {
            LOGGER.error("根据属性名获取属性值异常："+e.getMessage(),e);
            return null;
        }
    }
}
