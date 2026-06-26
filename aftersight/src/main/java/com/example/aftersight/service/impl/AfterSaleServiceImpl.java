package com.example.aftersight.service.impl;

import com.example.aftersight.common.Result;
import com.example.aftersight.dto.SubmitDTO;
import com.example.aftersight.entity.AfterSaleOrder;
import com.example.aftersight.entity.OrderInfo;
import com.example.aftersight.mapper.AfterSaleMapper;
import com.example.aftersight.service.AfterSaleService;
import com.example.aftersight.utils.ImageUploadUtils;
import com.example.aftersight.vo.SubmitVO;
import com.google.gson.Gson;
import jakarta.annotation.Resource;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AfterSaleServiceImpl implements AfterSaleService {

    @Resource
    private AfterSaleMapper afterSaleMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Autowired
    private ImageUploadUtils uploadUtils;

    // 前缀常量
    private static final String PREFIX = "SH";
    // Redis key 前缀
    private static final String REDIS_KEY_PREFIX = "ticket:seq:";
    // 日期格式化 yyyyMMdd
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String RATE_LIMIT_KEY_PREFIX = "rate:limit:token_bucket:";
    private static final String TOKEN_BUCKET_CAPACITY = "5";
    private static final String TOKEN_REFILL_INTERVAL_MILLIS = "60000";
    private static final DefaultRedisScript<Long> acquireToken;

    static {
        acquireToken = new DefaultRedisScript<>();
        acquireToken.setLocation(new ClassPathResource("token.lua"));
        acquireToken.setResultType(Long.class);
    }

    /**
     * 用户提交售后申请
     *
     * @param submitDTO
     * @return
     */
    @Override
    public Result<SubmitVO> submit(SubmitDTO submitDTO) {
        SubmitVO submitVO = new SubmitVO();
        List<String> imageUrl = new ArrayList<>();

        //订单号为空
        String orderNo = submitDTO.getOrderNo();
        if (orderNo == null || orderNo.isBlank()) {
            return Result.fail(400, "订单号不存在");
        }
        List<String> nos = afterSaleMapper.selectAllOrderNos();
        //布隆过滤器过滤不存在的订单
        RBloomFilter<Object> bloom = redissonClient.getBloomFilter("bloom:order_no");
        bloom.tryInit(1000000, 0.03);
        //addOrderNos(bloom, nos);  //todo 只执行一次，后面会注释掉这一行
        boolean contains = bloom.contains(orderNo);
        if (!contains) {
            return Result.fail(400, "订单号不存在");
        }

        //查MySQL订单
        OrderInfo order = afterSaleMapper.getOrder(orderNo);
        //订单不存在，返回失败
        if (order == null) {
            return Result.fail(400, "订单号不存在");
        }

        List<String> keys = Collections.singletonList(RATE_LIMIT_KEY_PREFIX + order.getUserId());
        //redis令牌桶限流
        //令牌桶一次请求里要做很多动作,因此使用Lua脚本确保操作的原子性
        Long execute = stringRedisTemplate.execute(
                acquireToken,
                keys,
                TOKEN_BUCKET_CAPACITY,//桶容量
                TOKEN_REFILL_INTERVAL_MILLIS,//60000，每 60 秒补充 1 个令牌
                String.valueOf(System.currentTimeMillis())//当前时间戳
        );

        if (execute == null || execute != 1L) {
            return Result.fail(429, "提交过于频繁，请稍后再试");
        }

        //遍历用户上传的图片并将其上传到阿里云OSS中
        MultipartFile[] files = submitDTO.getEvidenceFiles();
        if (files != null) {
            for (MultipartFile evidenceFile : files) {
                String image = uploadUtils.uploadImage(evidenceFile, evidenceFile.getOriginalFilename());
                imageUrl.add(image);
            }
        }

        String date = LocalDateTime.now().format(DATE_FORMATTER);
        String key = REDIS_KEY_PREFIX + date;
        //生成工单号
        Long seq = stringRedisTemplate.opsForValue().increment(key);
        String ticketNo = PREFIX + date + String.format("%03d", seq);
        submitVO.setTicketNo(ticketNo);

        //组装售后工单表
        //将售后记录插入数据库
        AfterSaleOrder afterSaleOrder = new AfterSaleOrder();
        afterSaleOrder.setTicketNo(ticketNo);
        afterSaleOrder.setOrderId(order.getId());
        afterSaleOrder.setOrderNo(orderNo);
        afterSaleOrder.setStoreId(order.getStoreId());
        afterSaleOrder.setUserId(order.getUserId());
        afterSaleOrder.setAfterSaleType(submitDTO.getAfterSaleType());
        afterSaleOrder.setApplyReason(submitDTO.getApplyReason());
        afterSaleOrder.setEvidenceImages(new Gson().toJson(imageUrl));
        afterSaleOrder.setApplyAmount(order.getPayAmount());
        afterSaleOrder.setAiAuditStatus(0);
        afterSaleOrder.setTicketStatus(0);
        afterSaleOrder.setRetryCount(0);

        afterSaleMapper.addAfterSaleOrder(afterSaleOrder);

        submitVO.setTicketStatus(0);
        submitVO.setEstimatedTime("30秒内出结果");
        submitVO.setMessage("售后申请提交成功！");
        return Result.success(submitVO);
    }


    //将所有订单号添加到布隆过滤器中
    public void addOrderNos(RBloomFilter<Object> bloom, List<String> nos) {
        for (String no : nos) {
            bloom.add(no);
        }
    }


    /**
     * 工单列表分页查询实现类
     */
    @Override
    public List<AfterSaleOrder> getAfterSaleOrder() {
        return afterSaleMapper.getAfterSaleOrder();
    }

}