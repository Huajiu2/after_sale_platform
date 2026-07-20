package com.example.aftersight.mapper;

import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.entity.AiAuditLog;
import com.example.aftersight.entity.OperationLog;
import com.example.aftersight.entity.OrderInfo;
import com.example.aftersight.vo.AfterSaleOrderListVO;
import com.example.aftersight.vo.OrderInfoVO;
import com.example.aftersight.vo.SubmitVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface AfterSaleMapper {

    @Select("select order_no from order_info")
    List<String> selectAllOrderNos();

    @Select("SELECT o.id,o.order_no,o.store_id,o.user_id, o.product_name, o.product_spec, o.pay_amount, o.order_time, s.store_name " +
            "FROM order_info o LEFT JOIN store_info s ON o.store_id = s.id " +
            "WHERE o.order_no = #{orderNo}")
    OrderInfo getOrder(String orderNo);

    @Select("SELECT o.id,o.order_no, o.product_name, o.product_spec, o.pay_amount, o.order_time, s.store_name " +
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

    @Update("UPDATE after_sale_order SET ticket_status = #{ticketStatus}, ai_audit_status = #{aiAuditStatus}, " +
            "manual_audit_by = #{manualAuditBy}, manual_audit_time = NOW(), " +
            "manual_remark = #{manualRemark}, manual_result = #{manualResult} " +
            "WHERE ticket_no = #{ticketNo} AND ticket_status = 2")
    int updateManualAudit(AfterSaleOrder afterSaleOrder);


    @Insert("INSERT INTO operation_log (biz_type, biz_id, operator, action, detail, ip_address, created_at) " +
            "VALUES (#{bizType}, #{bizId}, #{operator}, #{action}, #{detail}, #{ipAddress}, NOW())")
    void insertOperationLog(OperationLog log);

    // 更新工单的 AI 审核结果
    @Update("UPDATE after_sale_order SET ai_audit_result = #{aiAuditResult}, " +
            "ai_confidence = #{aiConfidence}, ai_audit_status = #{aiAuditStatus}, " +
            "ai_audit_time = NOW(), ticket_status = #{ticketStatus} " +
            "WHERE ticket_no = #{ticketNo}")
    void updateAiAudit(AfterSaleOrder afterSaleOrder);

    // 插入 AI 审核日志
    @Insert("INSERT INTO ai_audit_log (ticket_id, ticket_no, llm_model, llm_prompt, llm_response, " +
            "llm_latency_ms, rag_chunk_ids, audit_conclusion, confidence, suggested_action, created_at) " +
            "VALUES (#{ticketId}, #{ticketNo}, #{llmModel}, #{llmPrompt}, #{llmResponse}, " +
            "#{llmLatencyMs}, #{ragChunkIds}, #{auditConclusion}, #{confidence}, #{suggestedAction}, NOW())")
    void insertAiAuditLog(AiAuditLog auditLog);

    @Select("SELECT * FROM ai_audit_log WHERE ticket_no = #{ticketNo} ORDER BY created_at DESC LIMIT 1")
    AiAuditLog getAiAuditLog(String ticketNo);

    @Update("UPDATE after_sale_order SET assignee = #{assignee}, updated_at = NOW() " +
            "WHERE ticket_no = #{ticketNo} AND ticket_status = 2 " +
            "AND (assignee IS NULL OR assignee = '')")
    int assignByTicketNo( String ticketNo,  String assignee);
}
