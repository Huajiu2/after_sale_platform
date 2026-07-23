package com.example.aftersight.mapper;

import com.example.aftersight.vo.DailyRecordVO;
import com.example.aftersight.vo.MonthlySummaryVO;
import com.example.aftersight.vo.RateTrendRowVO;
import com.example.aftersight.vo.StoreRankingReportVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReportMapper {

    MonthlySummaryVO monthlySummary(@Param("month") String month);

    List<DailyRecordVO> dailyRecords(@Param("month") String month);

    List<StoreRankingReportVO> storeRanking(@Param("month") String month);

    List<RateTrendRowVO> rateTrend(@Param("months") Integer months);
}
