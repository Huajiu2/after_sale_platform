package com.example.aftersight.service;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.dto.BatchRetryIdsDTO;
import com.example.aftersight.vo.DlqMessageVO;

public interface DlqService {
    PageResult<DlqMessageVO> getDlqList(Integer page, Integer size, String ticketNo,
                                         String errorReason, String startTime, String endTime);

    Result retryById(Long id);

    Result batchRetry(BatchRetryIdsDTO dto);

    Result deleteById(Long id);
}
