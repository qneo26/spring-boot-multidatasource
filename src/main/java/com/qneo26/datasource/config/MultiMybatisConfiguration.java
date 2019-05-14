package com.qneo26.datasource.config;

import com.alibaba.druid.support.http.StatViewServlet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.boot.autoconfigure.MybatisProperties;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.mybatis.spring.mapper.ClassPathMapperScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.*;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *多mybatis实例
 * Created by qinhaiyu on 17-12-22.
 */
@Configuration
@EnableTransactionManagement(proxyTargetClass = true)
public class MultiMybatisConfiguration {

    @Bean
    public static MultiMybatisBeanFactoryProcessor multiMybatisBeanFactoryProcessor(){
        return new MultiMybatisBeanFactoryProcessor();
    }


    public static class MultiMybatisBeanFactoryProcessor implements BeanFactoryPostProcessor, EnvironmentAware, ResourceLoaderAware, ApplicationContextAware {

        private final static Logger logger= LoggerFactory.getLogger(MultiMybatisBeanFactoryProcessor.class);

        final ConversionService conversionService = new DefaultConversionService();

        protected ResourceLoader resourceLoader;

        @Autowired(required = false)
        protected Interceptor[] interceptors;

        @Autowired(required = false)
        protected DatabaseIdProvider databaseIdProvider;

        private ConfigurableEnvironment environment;

        private ApplicationContext applicationContext;
        /**
         *
         * @param beanFactory
         * @throws BeansException
         */
        @Override
        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            String[] instances = environment.getProperty("mybatis.instances",String[].class,new String[0]);

            for(String instance:instances){
                String instanceProperty="datasource."+instance;
                DataSourceProperties properties=bindToTarget(instanceProperty,bindToTarget("datasource.default",null, DataSourceProperties.class),null);
                DataSource dataSource= properties.initializeDataSourceBuilder().build();
                dataSource=bindToTarget(instanceProperty,bindToTarget("datasource.default",dataSource, null),null);
                DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);

                TransactionTemplate transactionTemplate= new TransactionTemplate(transactionManager);
                MybatisProperties mybatisProperties=bindToTarget("mybatis."+instance,bindToTarget("mybatis.default",null,MybatisProperties.class),null);

                SqlSessionFactory sqlSessionFactory=configSqlSessionFactory(mybatisProperties,dataSource);
                SqlSessionTemplate sqlSessionTemplate=getSqlSessionTemplate(mybatisProperties,sqlSessionFactory);

                beanFactory.registerSingleton("datasource-"+instance,dataSource);
                beanFactory.registerSingleton("transactionManager-"+instance,transactionManager);
                beanFactory.registerSingleton("transactionTemplate-"+instance,transactionTemplate);
                beanFactory.registerSingleton("sqlSessionFactory-"+instance,sqlSessionFactory);
                beanFactory.registerSingleton("sqlSessionTemplate-"+instance,sqlSessionTemplate);

                BeanDefinitionRegistry registry=getBeanDefinitionRegistry();

                ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
                scanner.setSqlSessionFactory(sqlSessionFactory);
                String [] packages= environment.getProperty("mybatis."+instance+".packages",String[].class,new String[0]);
                try {
                    if (this.resourceLoader != null) {
                        scanner.setResourceLoader(this.resourceLoader);
                    }
                    scanner.setAnnotationClass(Mapper.class);
                    scanner.registerFilters();
                    scanner.doScan(packages);

                } catch (IllegalStateException ex) {
                    logger.debug("Could not determine auto-configuration " + "package, automatic mapper scanning disabled.");
                }
            }

        }


        private BeanDefinitionRegistry getBeanDefinitionRegistry() {
            if (applicationContext instanceof BeanDefinitionRegistry) {
                return (BeanDefinitionRegistry) applicationContext;
            }
            if (applicationContext instanceof AbstractApplicationContext) {
                return (BeanDefinitionRegistry) ((AbstractApplicationContext) applicationContext)
                        .getBeanFactory();
            }
            throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
        }

        private  <T> T bindToTarget(String prefix,T t,Class<T> cls){
            PropertiesConfigurationFactory<T> binder = t==null?new PropertiesConfigurationFactory(cls):new PropertiesConfigurationFactory(t);
            binder.setTargetName(prefix);
            binder.setConversionService(this.conversionService);
            binder.setPropertySources(new FlatPropertySources(this.environment
                    .getPropertySources()));
            try {
                return binder.getObject();
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot bind to target", ex);
            }
        }

        @Override
        public void setEnvironment(Environment environment) {
            this.environment=(ConfigurableEnvironment)environment;
        }

        SqlSessionFactory configSqlSessionFactory(MybatisProperties properties, DataSource dataSource){
            SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
            factory.setDataSource(dataSource);
            factory.setVfs(SpringBootVFS.class);
            if (StringUtils.hasText(properties.getConfigLocation())) {
                factory.setConfigLocation(this.resourceLoader.getResource(properties.getConfigLocation()));
            }
            factory.setConfiguration(properties.getConfiguration());
            if (!ObjectUtils.isEmpty(this.interceptors)) {
                factory.setPlugins(this.interceptors);
            }
            if (this.databaseIdProvider != null) {
                factory.setDatabaseIdProvider(this.databaseIdProvider);
            }
            if (StringUtils.hasLength(properties.getTypeAliasesPackage())) {
                factory.setTypeAliasesPackage(properties.getTypeAliasesPackage());
            }
            if (StringUtils.hasLength(properties.getTypeHandlersPackage())) {
                factory.setTypeHandlersPackage(properties.getTypeHandlersPackage());
            }
            if (!ObjectUtils.isEmpty(properties.resolveMapperLocations())) {
                factory.setMapperLocations(properties.resolveMapperLocations());
            }

            try {
                return factory.getObject();
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("cann't initialize sqlSessionFactory",e);
            }
        }

        SqlSessionTemplate getSqlSessionTemplate(MybatisProperties mybatisProperties, SqlSessionFactory sqlSessionFactory){
            ExecutorType executorType = mybatisProperties.getExecutorType();
            if (executorType != null) {
                return new SqlSessionTemplate(sqlSessionFactory, executorType);
            } else {
                return new SqlSessionTemplate(sqlSessionFactory);
            }
        }

        @Override
        public void setResourceLoader(ResourceLoader resourceLoader) {

            this.resourceLoader=resourceLoader;
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext=applicationContext;
        }

        /**
         * Convenience class to flatten out a tree of property sources without losing the
         * reference to the backing data (which can therefore be updated in the background).
         */
        private static class FlatPropertySources implements PropertySources {

            private PropertySources propertySources;

            FlatPropertySources(PropertySources propertySources) {
                this.propertySources = propertySources;
            }

            @Override
            public Iterator<PropertySource<?>> iterator() {
                MutablePropertySources result = getFlattened();
                return result.iterator();
            }

            @Override
            public boolean contains(String name) {
                return get(name) != null;
            }

            @Override
            public PropertySource<?> get(String name) {
                return getFlattened().get(name);
            }

            private MutablePropertySources getFlattened() {
                MutablePropertySources result = new MutablePropertySources();
                for (PropertySource<?> propertySource : this.propertySources) {
                    flattenPropertySources(propertySource, result);
                }
                return result;
            }

            private void flattenPropertySources(PropertySource<?> propertySource,
                                                MutablePropertySources result) {
                Object source = propertySource.getSource();
                if (source instanceof ConfigurableEnvironment) {
                    ConfigurableEnvironment environment = (ConfigurableEnvironment) source;
                    for (PropertySource<?> childSource : environment.getPropertySources()) {
                        flattenPropertySources(childSource, result);
                    }
                } else {
                    result.addLast(propertySource);
                }
            }

        }
    }
    /**
     * druid管理配置
     */
    @Configuration
    @ConditionalOnClass(StatViewServlet.class)
    @ConditionalOnProperty(name = "druid.admin.enabled")
    public static class DruidManagementConfiguration {

        @Value("${druid.username}")
        private String username;
        @Value("${druid.password}")
        private String password;
        @Value("${druid.reset}")
        private String resetEnable;

        @Bean
        public ServletRegistrationBean servletRegistration() {
            StatViewServlet servlet = new StatViewServlet();
            ServletRegistrationBean registration = new ServletRegistrationBean();
            registration.setServlet(servlet);
            Map<String, String> initParams = new HashMap<>();
            initParams.put("resetEnable", resetEnable);
            initParams.put("loginUsername", username);
            initParams.put("loginPassword", password);
            registration.setInitParameters(initParams);
            registration.setUrlMappings(Arrays.asList("/druid/*"));
            return registration;
        }
    }
}
