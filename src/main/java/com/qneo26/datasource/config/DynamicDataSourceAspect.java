package com.qneo26.datasource.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: qinhaiyu
 * Date: 2019-04-28
 * Time: 15:22
 */
@Aspect
@Component
public class DynamicDataSourceAspect implements Ordered {
    /**
     * 切点: 所有配置 DataSource 注解的类和方法
     */
    @Pointcut("@annotation(com.qneo26.datasource.config.Datasource)")
    public void dataSourceMethodPointCut() {}

    @Pointcut("@within(com.qneo26.datasource.config.Datasource)")
    public void dataSourceTypePointCut(){}

    @Pointcut("dataSourceMethodPointCut()||dataSourceTypePointCut()")
    public void dataSourcePointCut(){}

    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        com.qneo26.datasource.config.Datasource  ds = method.getAnnotation(com.qneo26.datasource.config.Datasource.class);
        if(ds==null){
            ds=method.getDeclaringClass().getAnnotation(com.qneo26.datasource.config.Datasource.class);
        }

        // 通过判断 DataSource 中的值来判断当前方法应用哪个数据源
        MultiDataSourceConfiguration.DynamicDataSource.setDatasourceName(ds.value());
        try {
            return point.proceed();
        } finally {
            MultiDataSourceConfiguration.DynamicDataSource.clear();
        }
    }

    /**
     * org.springframework.transaction.annotation.EnableTransactionManagement#order()
     * transaction的order是最低级的
     * @return
     */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
