package com.example.aftersight.controller;

import com.example.aftersight.common.Result;
import com.example.aftersight.entity.StoreInfo;
import com.example.aftersight.enums.AfterSaleTypeEnum;
import com.example.aftersight.enums.TicketStatusEnum;
import com.example.aftersight.mapper.AfterSaleMapper;
import com.example.aftersight.mapper.BasicMapper;
import com.example.aftersight.vo.OrderInfoVO;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/basic")
public class BasicController {

    private final BasicMapper basicMapper;
    private final AfterSaleMapper afterSaleMapper;

    public BasicController(BasicMapper basicMapper, AfterSaleMapper afterSaleMapper) {
        this.basicMapper = basicMapper;
        this.afterSaleMapper = afterSaleMapper;
    }

    @GetMapping("/stores")
    public Result<List<Map<String, Object>>> getStores(@RequestParam(required = false) String keyword) {
        List<StoreInfo> list = basicMapper.selectStores(keyword);
        List<Map<String, Object>> result = list.stream().map(s -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", s.getId());
            map.put("storeName", s.getStoreName());
            map.put("storeCode", s.getStoreCode());
            return map;
        }).collect(Collectors.toList());
        return Result.success(result);
    }

    @GetMapping("/order/{orderNo}")
    public Result<OrderInfoVO> getOrder(@PathVariable String orderNo) {
        OrderInfoVO vo = afterSaleMapper.getOrderVO(orderNo);
        if (vo == null) return Result.fail(404, "订单不存在");
        return Result.success(vo);
    }

    @GetMapping("/after-sale-types")
    public Result<List<Map<String, Object>>> getAfterSaleTypes() {
        List<Map<String, Object>> list = Arrays.stream(AfterSaleTypeEnum.values()).map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("code", e.getCode());
            map.put("name", e.getDesc());
            return map;
        }).collect(Collectors.toList());
        return Result.success(list);
    }

    @GetMapping("/ticket-statuses")
    public Result<List<Map<String, Object>>> getTicketStatuses() {
        List<Map<String, Object>> list = Arrays.stream(TicketStatusEnum.values()).map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("code", e.getCode());
            map.put("name", e.getDesc());
            return map;
        }).collect(Collectors.toList());
        return Result.success(list);
    }
}
