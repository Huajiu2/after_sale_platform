package com.example.aftersight.service;

import com.example.aftersight.common.Result;
import com.example.aftersight.dto.SubmitDTO;
import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.vo.AfterSaleDetailVO;
import com.example.aftersight.vo.AfterSaleOrderListVO;
import com.example.aftersight.vo.SubmitVO;

import java.util.List;

public interface AfterSaleService {
    Result<SubmitVO> submit(SubmitDTO submitDTO);

    List<AfterSaleOrderListVO> getAfterSaleOrder();

    Result<AfterSaleDetailVO> getAfterSaleOrderDetail(String ticketNo);
}
