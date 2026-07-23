package com.example.aftersight.mapper;

import com.example.aftersight.entity.DeadLetterMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface DlqMapper {

    @Insert("INSERT INTO dead_letter_message (msg_id, queue_name, exchange_name, routing_key, " +
            "error_reason, msg_content, retry_count, max_retry, dlq_status, error_time, created_at) " +
            "VALUES (#{msgId}, #{queueName}, #{exchangeName}, #{routingKey}, " +
            "#{errorReason}, #{msgContent}, #{retryCount}, #{maxRetry}, #{dlqStatus}, #{errorTime}, NOW())")
    void insert(DeadLetterMessage message);

    List<DeadLetterMessage> selectList(@Param("ticketNo") String ticketNo,
                                       @Param("errorReason") String errorReason,
                                       @Param("startTime") String startTime,
                                       @Param("endTime") String endTime);

    @Select("SELECT * FROM dead_letter_message WHERE id = #{id}")
    DeadLetterMessage selectById(Long id);

    @Update("UPDATE dead_letter_message SET dlq_status = #{dlqStatus}, last_retry_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("dlqStatus") Integer dlqStatus);

    @Delete("DELETE FROM dead_letter_message WHERE id = #{id}")
    int deleteById(Long id);
}
