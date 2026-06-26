package com.example.aftersight.controller;

import com.example.aftersight.common.Result;
import com.example.aftersight.dto.SubmitDTO;
import com.example.aftersight.service.AfterSaleService;
import com.example.aftersight.vo.SubmitVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
}
