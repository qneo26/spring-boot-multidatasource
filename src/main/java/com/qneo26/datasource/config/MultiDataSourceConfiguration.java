package com.qneo26.datasource.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: qinhaiyu
 * Date: 2019-04-27
 * Time: 22:33
 */
@ConditionalOnProperty("spring.dynamicdatasource.instances")
@Configuration
public class MultiDataSourceConfiguration {
    @Bean
    public DynamicDataSource multiDataSource(Environment environment){
        String[] instances = environment.getProperty("spring.dynamicdatasource.instances",String[].class,new String[0]);
        Map<String, Object> defaultPropMap = new RelaxedPropertyResolver(environment)
                .getSubProperties("spring.dynamicdatasource.");

        Map<Object,Object> dataSourceMap=new HashMap<>(instances.length);

        for(String instance:instances){
            DruidDataSource ds=new DruidDataSource();
            Map<String, Object> propMap = new HashMap<>(new RelaxedPropertyResolver(environment)
                    .getSubProperties("spring.dynamicdatasource."+instance));
            defaultPropMap.forEach((key,value)->{
                propMap.putIfAbsent(key,value);
            });
            MutablePropertyValues propertyValues = new MutablePropertyValues(propMap);
            new RelaxedDataBinder(ds).bind(propertyValues);
            dataSourceMap.put(instance,ds);
        }

        return new DynamicDataSource(dataSourceMap);
    }


    /**
     * Created with IntelliJ IDEA.
     * Description:
     * User: qinhaiyu
     * Date: 2019-04-27
     * Time: 21:55
     */
    public static class DynamicDataSource extends AbstractRoutingDataSource {

        private final static ThreadLocal<String> DATASOURCE_NAME=new ThreadLocal<>();

        public DynamicDataSource(Map<Object, Object> dataSources){
            setTargetDataSources(dataSources);
        }

        @Override
        protected Object determineCurrentLookupKey() {
            return DATASOURCE_NAME.get();
        }

        /**
         * 设置dataSource的名字
         * @param name
         */
        public static void setDatasourceName(String name){
            DATASOURCE_NAME.set(name);
        }

        /**
         * 移除当前线程绑定的DataSource值
         */
        public static void clear(){
            DATASOURCE_NAME.remove();
        }
    }
}
