package com.example.aftersight.controller;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.entity.OperationLog;
import com.example.aftersight.mapper.LogMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/log")
public class LogController {

    private final LogMapper logMapper;

    public LogController(LogMapper logMapper) {
        this.logMapper = logMapper;
    }

    @GetMapping("/list")
    public Result<PageResult<OperationLog>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizId) {
        PageHelper.startPage(page, size);
        List<OperationLog> list = logMapper.selectList(bizType, bizId);
        PageInfo<OperationLog> pageInfo = new PageInfo<>(list);

        PageResult<OperationLog> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);
        result.setRecords(pageInfo.getList());
        result.setTotal(pageInfo.getTotal());
        result.setPages(pageInfo.getPages());
        return Result.success(result);
    }
}
