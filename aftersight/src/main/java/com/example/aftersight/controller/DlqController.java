package com.example.aftersight.controller;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.dto.BatchRetryIdsDTO;
import com.example.aftersight.service.DlqService;
import com.example.aftersight.vo.DlqMessageVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dlq")
public class DlqController {

    private final DlqService dlqService;

    public DlqController(DlqService dlqService) {
        this.dlqService = dlqService;
    }

    @GetMapping("/list")
    public Result<PageResult<DlqMessageVO>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String ticketNo,
            @RequestParam(required = false) String errorReason,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        PageResult<DlqMessageVO> result = dlqService.getDlqList(page, size, ticketNo, errorReason, startTime, endTime);
        return Result.success(result);
    }

    @PostMapping("/retry/{id}")
    public Result retry(@PathVariable Long id) {
        return dlqService.retryById(id);
    }

    @PostMapping("/batch-retry")
    public Result batchRetry(@RequestBody BatchRetryIdsDTO dto) {
        return dlqService.batchRetry(dto);
    }

    @DeleteMapping("/{id}")
    public Result delete(@PathVariable Long id) {
        return dlqService.deleteById(id);
    }
}
