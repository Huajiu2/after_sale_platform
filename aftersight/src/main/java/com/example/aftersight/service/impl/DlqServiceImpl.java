package com.example.aftersight.service.impl;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.dto.BatchRetryIdsDTO;
import com.example.aftersight.entity.DeadLetterMessage;
import com.example.aftersight.enums.DlqStatusEnum;
import com.example.aftersight.mapper.DlqMapper;
import com.example.aftersight.service.DlqService;
import com.example.aftersight.vo.DlqMessageVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DlqServiceImpl implements DlqService {

    @Resource
    private DlqMapper dlqMapper;

    private DlqMessageVO toVO(DeadLetterMessage msg) {
        DlqMessageVO vo = new DlqMessageVO();
        vo.setId(msg.getId());
        vo.setMsgId(msg.getMsgId());
        vo.setQueueName(msg.getQueueName());
        vo.setExchangeName(msg.getExchangeName());
        vo.setRoutingKey(msg.getRoutingKey());
        vo.setTicketNo(msg.getTicketNo());
        vo.setErrorReason(msg.getErrorReason());
        vo.setRetryCount(msg.getRetryCount());
        vo.setMaxRetry(msg.getMaxRetry());
        vo.setDlqStatus(msg.getDlqStatus());
        DlqStatusEnum statusEnum = DlqStatusEnum.fromCode(msg.getDlqStatus());
        vo.setDlqStatusDesc(statusEnum != null ? statusEnum.getDesc() : "");
        vo.setErrorTime(msg.getErrorTime());
        vo.setLastRetryTime(msg.getLastRetryTime());
        return vo;
    }

    @Override
    public PageResult<DlqMessageVO> getDlqList(Integer page, Integer size, String ticketNo,
                                                String errorReason, String startTime, String endTime) {
        PageHelper.startPage(page, size);
        List<DeadLetterMessage> list = dlqMapper.selectList(ticketNo, errorReason, startTime, endTime);
        PageInfo<DeadLetterMessage> pageInfo = new PageInfo<>(list);

        List<DlqMessageVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());

        PageResult<DlqMessageVO> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);
        result.setRecords(voList);
        result.setTotal(pageInfo.getTotal());
        result.setPages(pageInfo.getPages());
        return result;
    }

    @Override
    @Transactional
    public Result retryById(Long id) {
        DeadLetterMessage msg = dlqMapper.selectById(id);
        if (msg == null) return Result.fail(404, "死信记录不存在");
        if (msg.getDlqStatus() != 0) return Result.fail(400, "该死信已处理");

        dlqMapper.updateStatus(id, 1);

        HashMap<String, Object> data = new HashMap<>();
        data.put("msgId", msg.getMsgId());
        data.put("targetQueue", msg.getQueueName());
        return Result.success("消息已重新投递到原队列", data);
    }

    @Override
    @Transactional
    public Result batchRetry(BatchRetryIdsDTO dto) {
        if (dto.getIds() == null || dto.getIds().isEmpty()) {
            return Result.fail(400, "ID列表不能为空");
        }
        int success = 0;
        List<String> failDetails = new ArrayList<>();
        for (Long id : dto.getIds()) {
            DeadLetterMessage msg = dlqMapper.selectById(id);
            if (msg == null || msg.getDlqStatus() != 0) {
                failDetails.add("ID=" + id + ": 不存在或已处理");
                continue;
            }
            int rows = dlqMapper.updateStatus(id, 1);
            if (rows > 0) success++;
        }

        HashMap<String, Object> result = new HashMap<>();
        result.put("totalCount", dto.getIds().size());
        result.put("successCount", success);
        result.put("failCount", failDetails.size());
        result.put("failDetails", failDetails);
        return Result.success(result);
    }

    @Override
    @Transactional
    public Result deleteById(Long id) {
        int rows = dlqMapper.deleteById(id);
        if (rows == 0) return Result.fail(404, "死信记录不存在");
        return Result.success("死信消息已删除");
    }
}
