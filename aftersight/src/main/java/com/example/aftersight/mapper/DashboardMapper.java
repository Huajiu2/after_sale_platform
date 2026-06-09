package com.example.aftersight.mapper;

import com.example.aftersight.vo.StoreRankingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardMapper {
    List<StoreRankingVO> getrank();

    @Select("select count(*) from after_sale_order where date (created_at)=curdate()")
    Integer getTodayOrders();

    @Select("select count(*) from after_sale_order where ai_audit_status=1 and date(ai_audit_time)=curdate()")
    Integer getAiComplete();

    @Select("select count(*) from after_sale_order where ticket_status=2")
    Integer getPending();

    @Select("select count(*) from dead_letter_message where date(error_time)=curdate() and dlq_status=0")
    Integer getDlq();

    @Select("select count(*) from after_sale_order where date(created_at)=date_sub(curdate(), interval 1 day)")
    Integer getYesterdayOrders();

    @Select("select count(*) from after_sale_order where ai_audit_status=1 and date(ai_audit_time)=date_sub(curdate(), interval 1 day)")
    Integer getYesterdayAiComplete();

    @Select("select count(*) from after_sale_order where ticket_status=2 and date(created_at)<curdate()")
    Integer getYesterdayPending();

    @Select("select count(*) from dead_letter_message where date(error_time)=date_sub(curdate(), interval 1 day) and dlq_status=0")
    Integer getYesterdayDlq();

    List<Map<String, Object>> getTrendOrders();

    List<Map<String, Object>> getTrendAiCompleted();

    List<Map<String, Object>> getTrendPendingManual();

    @Select("select count(*) from after_sale_order where after_sale_type=1")
    Integer getTypeCount1();
    @Select("select count(*) from after_sale_order where after_sale_type=2")
    Integer getTypeCount2();

    @Select("select count(*) from after_sale_order where after_sale_type=3")
    Integer getTypeCount3();
}
