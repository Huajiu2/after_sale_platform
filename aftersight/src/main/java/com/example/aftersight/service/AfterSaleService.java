package com.example.aftersight.service;

import com.example.aftersight.common.Result;
import com.example.aftersight.dto.SubmitDTO;
import com.example.aftersight.vo.SubmitVO;

public interface AfterSaleService {
    Result<SubmitVO> submit(SubmitDTO submitDTO);
}
