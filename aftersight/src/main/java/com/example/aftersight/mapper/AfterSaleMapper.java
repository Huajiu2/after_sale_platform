package com.example.aftersight.mapper;

import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.entity.OrderInfo;
import com.example.aftersight.vo.SubmitVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AfterSaleMapper {

    @Select("select order_no from order_info")
    List<String> selectAllOrderNos();

    @Select("select * from order_info where order_no=#{orderNo}")
    OrderInfo getOrder(String orderNo);

    @Insert("insert into after_sale_order (\n" +
            "        ticket_no,\n" +
            "        order_id,\n" +
            "        order_no,\n" +
            "        store_id,\n" +
            "        user_id,\n" +
            "        after_sale_type,\n" +
            "        apply_reason,\n" +
            "        evidence_images,\n" +
            "        apply_amount,\n" +
            "        ai_audit_status,\n" +
            "        ticket_status,\n" +
            "        retry_count\n" +
            "    ) values (\n" +
            "        #{ticketNo},\n" +
            "        #{orderId},\n" +
            "        #{orderNo},\n" +
            "        #{storeId},\n" +
            "        #{userId},\n" +
            "        #{afterSaleType},\n" +
            "        #{applyReason},\n" +
            "        #{evidenceImages},\n" +
            "        #{applyAmount},\n" +
            "        #{aiAuditStatus},\n" +
            "        #{ticketStatus},\n" +
            "        #{retryCount}\n" +
            "    )")
    void addAfterSaleOrder(AfterSaleOrder afterSaleOrder);

    @Select("select * from after_sale_order order by created_at desc ")
    List<AfterSaleOrder> getAfterSaleOrder();

}
