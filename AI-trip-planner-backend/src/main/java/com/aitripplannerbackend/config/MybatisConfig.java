package com.aitripplannerbackend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置类。
 *
 * {@code @MapperScan} 告诉 MyBatis-Spring 去扫描指定包下面的所有 Mapper 接口，
 * 自动为它们创建代理实现并注册到 Spring 容器。
 *
 * 如果不加这个注解，你就得在每个 Mapper 接口上单独加 {@code @Mapper}。
 * 用 {@code @MapperScan} 统一扫描更方便，新增 Mapper 不用额外操作。
 */
@Configuration
@MapperScan("com.aitripplannerbackend.mapper")
public class MybatisConfig {
}
