package com.jwei.xzfit.utils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ReflexUtils {

    /**
     * 通过反射将对象转为map
     * @param object:对象
     * @return map
     */
    public static Map<String, Object> objectToMap(Object object){
        Map<String,Object> dataMap = new HashMap<>();
        Class<?> clazz = object.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                dataMap.put(field.getName(),field.get(object));
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return dataMap;
    }

}
