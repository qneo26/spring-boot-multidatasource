package com.qneo26.datasource.config;

import java.lang.annotation.*;

/**
 * Created with IntelliJ IDEA.
 * Description:要使用的数据源名称
 * User: qinhaiyu
 * Date: 2019-04-27
 * Time: 21:47
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
public @interface Datasource {
    String value() default "";
}
