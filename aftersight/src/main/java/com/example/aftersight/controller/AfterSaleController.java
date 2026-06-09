package com.example.aftersight.controller;

import com.example.aftersight.common.Result;
import com.example.aftersight.dto.SubmitDTO;
import com.example.aftersight.vo.SubmitVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/after-sale")
public class AfterSaleController {

    @PostMapping("/submit")
    public Result<SubmitVO> submit(@RequestBody SubmitDTO submitDTO){
        SubmitVO submitVO = new SubmitVO();

        return Result.success(submitVO);
    }
}
