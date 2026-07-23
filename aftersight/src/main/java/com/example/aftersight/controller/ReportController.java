package com.example.aftersight.controller;

import com.example.aftersight.common.PageResult;
import com.example.aftersight.common.Result;
import com.example.aftersight.mapper.ReportMapper;
import com.example.aftersight.vo.DailyRecordVO;
import com.example.aftersight.vo.MonthlySummaryVO;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import com.example.aftersight.vo.RateTrendRowVO;
import com.example.aftersight.vo.StoreRankingReportVO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportMapper reportMapper;

    public ReportController(ReportMapper reportMapper) {
        this.reportMapper = reportMapper;
    }

    /**
     * 6.1 月度统计总览
     */
    @GetMapping("/monthly-summary")
    public Result<MonthlySummaryVO> monthlySummary(@RequestParam String month) {
        MonthlySummaryVO vo = reportMapper.monthlySummary(month);
        if (vo == null || vo.getTotalOrders() == null || vo.getTotalOrders() == 0) {
            MonthlySummaryVO empty = new MonthlySummaryVO();
            empty.setMonth(month);
            return Result.success(empty);
        }
        int total = vo.getTotalOrders();
        vo.setAiProcessRate(roundPct(vo.getAiProcessed(), total));
        vo.setManualInterventionRate(roundPct(vo.getManualIntervention(), total));
        vo.setApprovedRate(roundPct(vo.getApprovedCount(), total));
        vo.setRejectedRate(roundPct(vo.getRejectedCount(), total));
        vo.setClosedRate(roundPct(vo.getClosedCount(), total));
        return Result.success(vo);
    }

    /**
     * 6.2 每日统计明细
     */
    @GetMapping("/daily")
    public Result<Map<String, Object>> daily(@RequestParam String month) {
        List<DailyRecordVO> records = reportMapper.dailyRecords(month);
        for (DailyRecordVO r : records) {
            if (r.getTotalOrders() != null && r.getTotalOrders() > 0) {
                r.setAiProcessRate(roundPct(r.getAiCompleted(), r.getTotalOrders()));
                r.setRejectedRate(roundPct(r.getRejected(), r.getTotalOrders()));
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        return Result.success(result);
    }

    /**
     * 6.3 月度店铺排行（分页）
     */
    @GetMapping("/store-ranking")
    public Result<PageResult<StoreRankingReportVO>> storeRanking(
            @RequestParam String month,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        List<StoreRankingReportVO> all = reportMapper.storeRanking(month);

        int from = (page - 1) * size;
        int to = Math.min(from + size, all.size());

        List<StoreRankingReportVO> records = new ArrayList<>();
        for (int i = from; i < to; i++) {
            StoreRankingReportVO item = all.get(i);
            item.setRank(i + 1);
            item.setTrend(0.0);
            records.add(item);
        }

        PageResult<StoreRankingReportVO> result = new PageResult<>();
        result.setPage(page);
        result.setSize(size);
        result.setRecords(records);
        result.setTotal((long) all.size());
        result.setPages((int) Math.ceil((double) all.size() / size));
        return Result.success(result);
    }

    /**
     * 6.4 通过率/驳回率趋势
     */
    @GetMapping("/rate-trend")
    public Result<Map<String, Object>> rateTrend(@RequestParam(defaultValue = "6") Integer months) {
        List<RateTrendRowVO> raw = reportMapper.rateTrend(months);

        List<String> labels = new ArrayList<>();
        List<Double> approveRate = new ArrayList<>();
        List<Double> rejectRate = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Set<String> dbMonths = raw.stream().map(RateTrendRowVO::getLabel).collect(Collectors.toSet());

        for (int i = months - 1; i >= 0; i--) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MONTH, -i);
            String label = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
            labels.add(label);

            if (dbMonths.contains(label)) {
                RateTrendRowVO row = raw.stream().filter(r -> label.equals(r.getLabel())).findFirst().orElse(null);
                if (row != null) {
                    int total = row.getTotal() != null ? row.getTotal() : 0;
                    int rejected = row.getRejected() != null ? row.getRejected() : 0;
                    int approved = total - rejected;
                    approveRate.add(total > 0 ? Math.round((double) approved / total * 1000.0) / 10.0 : 0.0);
                    rejectRate.add(total > 0 ? Math.round((double) rejected / total * 1000.0) / 10.0 : 0.0);
                }
            } else {
                approveRate.add(0.0);
                rejectRate.add(0.0);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("approveRate", approveRate);
        result.put("rejectRate", rejectRate);
        return Result.success(result);
    }

    /**
     * 6.5 导出报表
     */
    @GetMapping("/export")
    public void export(@RequestParam String type, @RequestParam String month, HttpServletResponse response) throws Exception {
        String fileName = URLEncoder.encode(month + "月度报表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("月度报表");

        // 月度总览
        MonthlySummaryVO summary = reportMapper.monthlySummary(month);

        String[] headers = {"指标", "数值"};
        XSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) headerRow.createCell(i).setCellValue(headers[i]);

        Object[][] data = {
                {"售后单总数", summary != null ? summary.getTotalOrders() : 0},
                {"AI自动处理数", summary != null ? summary.getAiProcessed() : 0},
                {"AI处理率(%)", summary != null ? summary.getAiProcessRate() : 0},
                {"人工介入数", summary != null ? summary.getManualIntervention() : 0},
                {"已通过数", summary != null ? summary.getApprovedCount() : 0},
                {"已驳回数", summary != null ? summary.getRejectedCount() : 0},
                {"驳回率(%)", summary != null ? summary.getRejectedRate() : 0}
        };
        for (int i = 0; i < data.length; i++) {
            XSSFRow row = sheet.createRow(i + 1);
            row.createCell(0).setCellValue((String) data[i][0]);
            row.createCell(1).setCellValue(((Number) data[i][1]).doubleValue());
        }
        for (int i = 0; i < 2; i++) sheet.autoSizeColumn(i);

        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private Double roundPct(Integer part, Integer total) {
        if (part == null || total == null || total == 0) return 0.0;
        return Math.round((double) part / total * 1000.0) / 10.0;
    }
}
