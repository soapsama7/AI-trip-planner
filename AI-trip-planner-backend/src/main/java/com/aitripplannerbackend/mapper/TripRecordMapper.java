package com.aitripplannerbackend.mapper;

import com.aitripplannerbackend.entity.TripRecordEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 旅行记录的 MyBatis Mapper 接口。
 *
 * <h3>什么是 Mapper？</h3>
 * Mapper 是 MyBatis 中用来操作数据库的接口。你只需要写接口方法 + SQL 注解，
 * MyBatis 会在运行时自动生成实现类（动态代理），你不用写任何实现代码。
 *
 * <h3>注解方式 vs XML 方式</h3>
 * MyBatis 写 SQL 有两种方式：
 * 1. 注解方式（本项目用的）：直接在方法上用 @Insert、@Select 写 SQL，简单直观
 * 2. XML 方式（黑马点评用的）：SQL 写在 XML 文件里，适合复杂 SQL
 *
 * <h3>#{} 占位符</h3>
 * SQL 中的 #{taskId} 是 MyBatis 的参数占位符，相当于 JDBC 的 PreparedStatement 的 ?，
 * 可以防止 SQL 注入。MyBatis 会自动把方法参数的对应属性值填进去。
 */
@Mapper
public interface TripRecordMapper {

    /**
     * 插入一条旅行记录。
     * 任务完成后，TripPlannerServiceImpl 的 persist() 方法会调用这里把结果存入数据库。
     *
     * @param entity 要插入的记录实体
     * @return 影响的行数（正常情况下为 1）
     */
    @Insert("""
            INSERT INTO trip_record
            (task_id, city, travel_time, budget, request_json, plan_json, total_cost)
            VALUES
            (#{taskId}, #{city}, #{travelTime}, #{budget}, #{requestJson}, #{planJson}, #{totalCost})
            """)
    int insert(TripRecordEntity entity);

    /**
     * 分页查询历史记录，按 id 倒序（最新的排最前面）。
     *
     * LIMIT #{limit} OFFSET #{offset} 是 MySQL 的分页语法：
     * - OFFSET：跳过前面多少条（比如第 2 页、每页 10 条，offset = 10）
     * - LIMIT：最多返回多少条
     *
     * @param offset 偏移量，由 (page - 1) * size 计算得出
     * @param limit  每页数量
     * @return 历史记录列表
     */
    @Select("""
            SELECT id, task_id, city, travel_time, budget, request_json, plan_json, total_cost, created_at, updated_at
            FROM trip_record
            ORDER BY id DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<TripRecordEntity> selectHistory(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM trip_record")
    long countHistory();

    @Select("SELECT COUNT(*) FROM trip_record WHERE task_id = #{taskId}")
    long countByTaskId(@Param("taskId") String taskId);
}
