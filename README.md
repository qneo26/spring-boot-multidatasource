# Getting Started

### 简单说明
使用spring-boot过程中遇到了多个数据源问题，项目使用的是mybatis，参照`org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration`和网上大佬们的思路，
实现了两种多数据源使用mybatis的方式


#### MultiDataSourceConfiguration
    spring.dynamicdatasource.instances=db1,db2
    spring.dynamicdatasource.driverClassName = com.mysql.jdbc.Driver
    spring.dynamicdatasource.initialSize=5
    spring.dynamicdatasource.maxActive=20
    spring.dynamicdatasource.maxWait=60000
    spring.dynamicdatasource.minIdle=3
    
    spring.dynamicdatasource.timeBetweenEvictionRunsMillis=60000
    spring.dynamicdatasource.minEvictableIdleTimeMillis=300000
    spring.dynamicdatasource.testWhileIdle=true
    spring.dynamicdatasource.testOnBorrow=false
    spring.dynamicdatasource.testOnReturn=false
    spring.dynamicdatasource.poolPreparedStatements=true
    
    spring.dynamicdatasource.maxOpenPreparedStatements=20
    spring.dynamicdatasource.maxIdleTime=180000
    spring.dynamicdatasource.validationQuery=select 4
    spring.dynamicdatasource.filters=stat
    
    spring.dynamicdatasource.removeAbandoned=true
    spring.dynamicdatasource.removeAbandonedTimeoutMillis=120000
    spring.dynamicdatasource.logAbandoned=true
    
    spring.dynamicdatasource.db1.url = jdbc:mysql://192.168.153.34:3306/db1
    spring.dynamicdatasource.db1.username = root
    spring.dynamicdatasource.db1.password = 123
    
    spring.dynamicdatasource.db2.url = jdbc:mysql://192.168.153.34:3306/db2
    spring.dynamicdatasource.db2.username = root
    spring.dynamicdatasource.db2.password = 123
    ########mybatis 这里跟spring-boot-mybatis配置一致################
    mybatis.configuration.useColumnLabel=true
    mybatis.configuration.mapUnderscoreToCamelCase=true
    mybatis.mapper-locations = classpath:mapper/*Mapper.xml
    
 原理是`AbstractRoutingDataSource`，配置和DataSourceAutoConfiguration是一致的，每个datasource实例可以覆盖默认配置，mybatis的实例只有一个，所以可以利用上`mybatisAutoconfiguration`，使用`@Datasource`注解来动态切换数据源
 
 ####MultiMybatisConfiguration
 
 多个datasource，多个mybatis实例,如果是从多个项目融合到一起的，可以多个mybatis配置共存
 
     ####常规的连接池默认配置，可以被覆盖
     datasource.default.driverClassName = com.mysql.jdbc.Driver
     datasource.default.type=com.alibaba.druid.pool.DruidDataSource
     datasource.default.initialSize=5
     datasource.default.maxActive=20
     datasource.default.maxWait=60000
     datasource.default.minIdle=3
     
     datasource.default.timeBetweenEvictionRunsMillis=60000
     datasource.default.minEvictableIdleTimeMillis=300000
     datasource.default.testWhileIdle=true
     datasource.default.testOnBorrow=false
     datasource.default.testOnReturn=false
     datasource.default.poolPreparedStatements=true
     
     datasource.default.maxOpenPreparedStatements=20
     datasource.default.validationQuery=select 4
     datasource.default.filters=stat
     
     datasource.default.removeAbandoned=true
     datasource.default.removeAbandonedTimeoutMillis=120000
     datasource.default.logAbandoned=true
     
     
     datasource.db1.url = jdbc:mysql://192.168.153.34:3306/db1
     datasource.db1.username = root
     datasource.db1.password = 123
     
     
     datasource.db2.url = jdbc:mysql://192.168.153.34:3306/db2
     datasource.db2.username = root
     datasource.db2.password = 123
     ########mybatis################
     mybatis.default.configuration.useColumnLabel=true
     mybatis.default.configuration.mapUnderscoreToCamelCase=true
     
     ###这里必须分开扫描，有几个mytatis实例，扫描几次，如果不分开扫描，同一个mapper会被多次注册，spring会报错
     mybatis.db1.packages=com.qneo26.datasource.example.mapper.db1
     mybatis.db2.packages=com.qneo26.datasource.example.mapper.db2
     
     mybatis.default.mapper-locations = classpath:mapper/*Mapper.xml
     mybatis.instances=db1,db2



