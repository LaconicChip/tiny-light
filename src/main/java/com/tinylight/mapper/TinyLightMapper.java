package com.tinylight.mapper;

import com.tinylight.entity.TinyLight;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    @Update("UPDATE tiny_light SET content = #{content}, mood = #{mood} WHERE id = #{id}")
    int update(TinyLight t);

    @Delete("DELETE FROM tiny_light WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM tiny_light WHERE user_id = #{userId}")
    int countAllByUserId(@Param("userId") String userId);

    @Select("SELECT COUNT(*) FROM tiny_light WHERE user_id = #{userId} AND YEAR(light_date) = #{year}")
    int countByUserIdAndYear(@Param("userId") String userId, @Param("year") int year);

    @Select("SELECT mood, COUNT(*) AS cnt FROM tiny_light " +
            "WHERE user_id = #{userId} AND mood IS NOT NULL GROUP BY mood")
    List<Map<String, Object>> selectMoodDistribution(@Param("userId") String userId);

    @Select("SELECT light_date FROM tiny_light WHERE user_id = #{userId} ORDER BY light_date")
    List<LocalDate> selectAllDatesByUserId(@Param("userId") String userId);

    @Select("SELECT * FROM tiny_light WHERE user_id = #{userId} " +
            "ORDER BY light_date DESC LIMIT #{offset}, #{size}")
    List<TinyLight> selectByUserIdPaged(@Param("userId") String userId,
                                        @Param("offset") int offset,
                                        @Param("size") int size);
}
