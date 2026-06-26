package com.example.aftersight.controller;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.dto.SubmitDTO;
import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.service.AfterSaleService;
import com.example.aftersight.vo.SubmitVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/after-sale")
public class AfterSaleController {

    @Autowired
    private AfterSaleService afterSaleService;

    /**
     * 用户提交售后申请
     * @param submitDTO
     * @return
     */
    @PostMapping(value = "/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<SubmitVO> submit(@ModelAttribute SubmitDTO submitDTO){
        return afterSaleService.submit(submitDTO);
    }

    /**
     * 工单列表分页查询
     */
    @GetMapping("/list")
    public Result<PageResult> getAfterSaleOrder(@RequestParam(defaultValue = "1") Integer page,
                                                @RequestParam(defaultValue = "10") Integer size){
        PageHelper.startPage(page,size);//开启分页查询
        List<AfterSaleOrder> afterSaleOrders=afterSaleService.getAfterSaleOrder();
        PageInfo<AfterSaleOrder> pageInfo = new PageInfo<>(afterSaleOrders);
        PageResult<AfterSaleOrder> pageResult = new PageResult<>();

        pageResult.setPage(page);
        pageResult.setSize(size);
        pageResult.setRecords(pageInfo.getList());
        pageResult.setPages(pageInfo.getPages());
        pageResult.setTotal(pageInfo.getTotal());
        return Result.success(pageResult);
    }
}
