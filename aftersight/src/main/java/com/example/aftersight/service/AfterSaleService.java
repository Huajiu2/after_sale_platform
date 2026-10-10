package com.example.aftersight.service;

import com.example.aftersight.common.Result;
import com.example.aftersight.dto.AfterSaleQueryDTO;
import com.example.aftersight.dto.BatchAssignDTO;
import com.example.aftersight.dto.ManualAuditDTO;
import com.example.aftersight.dto.SubmitDTO;
import com.example.aftersight.vo.AfterSaleDetailVO;
import com.example.aftersight.vo.AfterSaleOrderListVO;
import com.example.aftersight.vo.SubmitVO;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

public interface AfterSaleService {
    Result<SubmitVO> submit(SubmitDTO submitDTO);

    List<AfterSaleOrderListVO> getAfterSaleOrder(AfterSaleQueryDTO query);

    Result<AfterSaleDetailVO> getAfterSaleOrderDetail(String ticketNo);


    Result manualAuditSubmit(ManualAuditDTO auditDTO);

    Result batchAssign(BatchAssignDTO assignDTO);

    Result batchRetry(BatchAssignDTO dto);

    void export(HttpServletResponse response) throws IOException;
}
