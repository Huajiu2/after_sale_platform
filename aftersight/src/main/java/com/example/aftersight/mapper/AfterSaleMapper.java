package com.example.aftersight.mapper;

import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.entity.OrderInfo;
import com.example.aftersight.vo.AfterSaleOrderListVO;
import com.example.aftersight.vo.OrderInfoVO;
import com.example.aftersight.vo.SubmitVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface AfterSaleMapper {

    @Select("select order_no from order_info")
    List<String> selectAllOrderNos();

    @Select("SELECT o.order_no, o.product_name, o.product_spec, o.pay_amount, o.order_time, s.store_name " +
            "FROM order_info o LEFT JOIN store_info s ON o.store_id = s.id " +
            "WHERE o.order_no = #{orderNo}")
    OrderInfo getOrder(String orderNo);

    @Select("SELECT o.order_no, o.product_name, o.product_spec, o.pay_amount, o.order_time, s.store_name " +
            "FROM order_info o LEFT JOIN store_info s ON o.store_id = s.id " +
            "WHERE o.order_no = #{orderNo}")
    OrderInfoVO getOrderVO(String orderNo);

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

    @Select("    select\n" +
            "        a.ticket_no as ticketNo,\n" +
            "        a.order_no as orderNo,\n" +
            "        a.store_id as storeId,\n" +
            "        s.store_name as storeName,\n" +
            "        a.after_sale_type as afterSaleType,\n" +
            "        a.apply_reason as applyReason,\n" +
            "        a.evidence_images as evidenceImages,\n" +
            "        a.ai_audit_result as aiAuditResult,\n" +
            "        a.ai_confidence as aiConfidence,\n" +
            "        a.ticket_status as ticketStatus,\n" +
            "        a.created_at as createdAt\n" +
            "    from after_sale_order a\n" +
            "    left join store_info s on a.store_id = s.id\n" +
            "    order by a.created_at desc")
    List<AfterSaleOrderListVO> getAfterSaleOrder();

    @Select("select order_no from after_sale_order where ticket_no=#{ticketNo}")
    String getOrderNo(String ticketNo);

    @Select("select * from after_sale_order where ticket_no=#{ticketNo}")
    AfterSaleOrder getByTicketNo(String ticketNo);
}
