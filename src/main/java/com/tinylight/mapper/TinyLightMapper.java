package com.tinylight.mapper;

import com.tinylight.entity.TinyLight;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface TinyLightMapper {

    @Insert("INSERT INTO tiny_light(user_id, content, mood, light_date, created_at) " +
            "VALUES(#{userId}, #{content}, #{mood}, #{lightDate}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TinyLight t);

    @Select("SELECT COUNT(*) FROM tiny_light WHERE user_id = #{userId} AND light_date = #{lightDate}")
    int countByUserIdAndDate(@Param("userId") String userId, @Param("lightDate") LocalDate lightDate);

    @Select("SELECT * FROM tiny_light WHERE user_id = #{userId} AND light_date = #{lightDate}")
    TinyLight selectByUserIdAndDate(@Param("userId") String userId, @Param("lightDate") LocalDate lightDate);

    @Select("SELECT * FROM tiny_light WHERE user_id = #{userId} AND light_date BETWEEN #{start} AND #{end} ORDER BY light_date")
    List<TinyLight> selectByUserIdAndDateRange(@Param("userId") String userId,
                                                @Param("start") LocalDate start,
                                                @Param("end") LocalDate end);

    @Select("SELECT * FROM tiny_light WHERE id = #{id}")
    TinyLight selectById(@Param("id") Long id);
}
