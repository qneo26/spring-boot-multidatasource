package com.qneo26.datasource.example.service;

import com.qneo26.datasource.config.Datasource;
import com.qneo26.datasource.example.mapper.db1.DB1Mapper;

import javax.annotation.Resource;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: qinhaiyu
 * Date: 2019-05-14
 * Time: 14:39
 */
@Datasource("db1")
public class HelloService {

    @Resource
    DB1Mapper db1Mapper;

    @Datasource("db2")
    public void hi(){

    }
}
