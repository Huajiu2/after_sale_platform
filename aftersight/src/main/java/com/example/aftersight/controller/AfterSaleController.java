package com.example.aftersight.controller;

import com.example.aftersight.common.Result;
import com.example.aftersight.dto.SubmitDTO;
import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.service.AfterSaleService;
import com.example.aftersight.vo.SubmitVO;
import com.github.pagehelper.PageHelper;
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
    @GetMapping("/list/{page}/{size}")
    public Result getAfterSaleOrder(@PathVariable Integer page,
                                    @PathVariable Integer size){
        PageHelper.startPage(page,size);//开启分页查询
        List<AfterSaleOrder> afterSaleOrders=afterSaleService.getAfterSaleOrder();


    }
}
