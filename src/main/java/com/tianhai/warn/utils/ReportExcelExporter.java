package com.tianhai.warn.utils;

import com.tianhai.warn.model.CalculationResult;
import com.tianhai.warn.model.LateReturn;
import jakarta.servlet.ServletOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.tianhai.warn.vo.CollegeLateReturnStatVO;
import com.tianhai.warn.vo.DormitoryLateReturnStatVO;
import com.tianhai.warn.vo.ReportCardStatVO;
import com.tianhai.warn.vo.WeekLateReturnStatVO;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map; // 导入 Map
// todo 以宿舍门牌号作为维度的数据导出

public class ReportExcelExporter {
    // 私有构造函数，防止实例化
    private ReportExcelExporter() {}

    /**
     * 创建整个报表统计的 Excel Workbook
     * @param reportCardStatVO         统计卡片数据 (假设类型为 ReportCardStatVO 或 Map<String, Object>)
     * @param calculationResult 高危预警计算结果 (假设类型为 CalculationResult 或 Map<String, Object>)
     * @param weekLateReturnStatVOList        晚归趋势数据
     * @param collegeLateReturnStatVOList       学院分布数据
     * @param dormitoryLateReturnStatMap 宿舍楼统计数据
     * @param startDate         筛选条件：开始日期
     * @param endDate           筛选条件：结束日期
     * @param college           筛选条件：学院
     * @param dormitoryBuilding 筛选条件：宿舍楼
     * @return 生成的 Workbook
     */
    public static Workbook createReportWorkbook(
            ReportCardStatVO reportCardStatVO,
            CalculationResult calculationResult,
            List<WeekLateReturnStatVO> weekLateReturnStatVOList,
            List<CollegeLateReturnStatVO> collegeLateReturnStatVOList,
            Map<String, List<DormitoryLateReturnStatVO>> dormitoryLateReturnStatMap,
            String startDate,
            String endDate,
            String college,
            String dormitoryBuilding) {

        Workbook workbook = new XSSFWorkbook();

        // 创建并填充 Sheet 1: 报表概览
        createSummarySheet(workbook, reportCardStatVO, calculationResult, startDate, endDate, college, dormitoryBuilding);

        // 创建并填充 Sheet 2: 晚归趋势
        createTrendSheet(workbook, weekLateReturnStatVOList, startDate, endDate, college, dormitoryBuilding);

        // 创建并填充 Sheet 3: 学院分布
        createCollegeSheet(workbook, collegeLateReturnStatVOList, startDate, endDate, college, dormitoryBuilding);

        // 创建并填充 Sheet 4: 宿舍楼统计
        createDormBuildingSheet(workbook, dormitoryLateReturnStatMap, startDate, endDate, college, dormitoryBuilding);


        return workbook;
    }

    /**
     * 创建并填充 Sheet 1: 报表概览
     * 
     * @param workbook          Workbook 对象
     * @param reportCardStatVO          统计卡片数据
     * @param calculationResult 高危预警计算结果
     * @param startDate         筛选条件：开始日期
     * @param endDate           筛选条件：结束日期
     * @param college           筛选条件：学院
     * @param dormitoryBuilding 筛选条件：宿舍楼
     */
    private static void createSummarySheet(
            Workbook workbook,
            ReportCardStatVO  reportCardStatVO, // 假设统计卡片 Service 返回 Map
            CalculationResult calculationResult, // 假设高危预警 Service 返回 Map
            String startDate,
            String endDate,
            String college,
            String dormitoryBuilding) {

        Sheet sheet = workbook.createSheet("报表概览"); // 创建 Sheet

        // --- 设置单元格样式（可选，为了美观） ---
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        // --- 写入内容 ---

        // A1: 报表统计概览 (主标题)
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("报表统计概览");
        titleCell.setCellStyle(boldStyle); // 应用粗体样式
        // 可以合并单元格以使标题居中，但此处简化

        // A3: 筛选条件:
        Row filterTitleRow = sheet.createRow(2);
        Cell filterTitleCell = filterTitleRow.createCell(0);
        filterTitleCell.setCellValue("筛选条件:");
        filterTitleCell.setCellStyle(boldStyle);

        // 筛选条件详情 (A4:B6)
        Row dateRangeRow = sheet.createRow(3);
        dateRangeRow.createCell(0).setCellValue("日期范围:");
        dateRangeRow.createCell(1).setCellValue((startDate != null ? startDate : "全部") + " 至 " + (endDate != null ? endDate : "全部"));

        Row collegeRow = sheet.createRow(4);
        collegeRow.createCell(0).setCellValue("学院:");
        collegeRow.createCell(1).setCellValue(college != null ? college : "全部");

        Row dormitoryRow = sheet.createRow(5);
        dormitoryRow.createCell(0).setCellValue("宿舍楼:");
        dormitoryRow.createCell(1).setCellValue(dormitoryBuilding != null ? dormitoryBuilding : "全部");

        // A8: 统计数据:
        Row statsTitleRow = sheet.createRow(7);
        Cell statsTitleCell = statsTitleRow.createCell(0);
        statsTitleCell.setCellValue("统计数据:");
        statsTitleCell.setCellStyle(boldStyle);

        // 统计数据表头 (A9:B9)
        Row statsHeaderRow = sheet.createRow(8);
        statsHeaderRow.createCell(0).setCellValue("统计项");
        statsHeaderRow.createCell(1).setCellValue("数值");
        statsHeaderRow.getCell(0).setCellStyle(boldStyle); // 表头也加粗
        statsHeaderRow.getCell(1).setCellStyle(boldStyle);

        // --- 统计数据内容 (A10:B13) ---

        // A10: 总晚归次数 | B10: [总晚归次数数值]
        Row totalLateRow = sheet.createRow(9);
        totalLateRow.createCell(0).setCellValue("总晚归次数");
        // 假设 cardData 中有一个 key 为 "totalLateReturns"，值为 Long 或 Integer
        Object totalLateReturns = reportCardStatVO != null
                ? reportCardStatVO.getTotalLateReturns()
                : null;
        totalLateRow.createCell(1).setCellValue(totalLateReturns != null ? totalLateReturns.toString() : "-"); // 安全获取并转为字符串

        // A11: 晚归学生数 | B11: [晚归学生数数值]
        Row lateStudentRow = sheet.createRow(10);
        lateStudentRow.createCell(0).setCellValue("晚归学生数");
        // 假设 cardData 中有一个 key 为 "lateStudentCount"，值为 Long 或 Integer
        Object lateStudentCount = reportCardStatVO != null
                ? reportCardStatVO.getLateStudentCount()
                : null;
        lateStudentRow.createCell(1).setCellValue(lateStudentCount != null ? lateStudentCount.toString() : "-"); // 安全获取并转为字符串

        // A12: 高危预警数 | B12: [高危预警数数值]
        Row highRiskRow = sheet.createRow(11);
        highRiskRow.createCell(0).setCellValue("高危预警数");
        Object highRiskCount = calculationResult != null
                ? calculationResult.getCount()
                : null; // 假设 CalculationResult 返回 Map 且 key 是 "count"
        highRiskRow.createCell(1).setCellValue(highRiskCount != null ? highRiskCount.toString() : "-"); // 安全获取并转为字符串

        // A13: 处理完成率 | B13: [处理完成率百分比]
        Row completionRateRow = sheet.createRow(12);
        completionRateRow.createCell(0).setCellValue("处理完成率");
        // 假设 cardData 中有一个 key 为 "completionRate"，值为 String (已经带 %)
        Object completionRate = reportCardStatVO != null
                ? reportCardStatVO.getCompletionRate()
                : null;
        completionRateRow.createCell(1).setCellValue(completionRate != null ? completionRate.toString() : "-"); // 安全获取并直接使用字符串

        // --- 自动调整列宽（可选） ---
        sheet.autoSizeColumn(0); // 自动调整第一列宽度
        sheet.autoSizeColumn(1); // 自动调整第二列宽度
        // 如果筛选条件列太长，可以适当调整合并或手动设置宽度
        // sheet.setColumnWidth(1, 5000); // 例如设置B列宽度
    }

    /**
     * 创建并填充 Sheet 2: 晚归趋势
     * @param workbook          Workbook 对象
     * @param weekLateReturnStatVOList         晚归趋势数据 (List<WeekLateReturnStatVO>)
     * @param startDate         筛选条件：开始日期
     * @param endDate           筛选条件：结束日期
     * @param college           筛选条件：学院
     * @param dormitoryBuilding 筛选条件：宿舍楼
     */
    private static void createTrendSheet(
            Workbook workbook,
            List<WeekLateReturnStatVO> weekLateReturnStatVOList, // TODO: 替换为你的 WeekLateReturnStatVO 列表类型
            String startDate,
            String endDate,
            String college,
            String dormitoryBuilding) {

        Sheet sheet = workbook.createSheet("晚归趋势"); // 创建 Sheet

        // --- 设置单元格样式（可选） ---
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        // --- 写入内容 ---

        // A1: 晚归趋势统计 (主标题)
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("晚归趋势统计");
        titleCell.setCellStyle(boldStyle);

        // A3: 筛选条件:
        Row filterTitleRow = sheet.createRow(2);
        Cell filterTitleCell = filterTitleRow.createCell(0);
        filterTitleCell.setCellValue("筛选条件:");
        filterTitleCell.setCellStyle(boldStyle);

        // 筛选条件详情 (A4:B6) - 同 Sheet 1
        Row dateRangeRow = sheet.createRow(3);
        dateRangeRow.createCell(0).setCellValue("日期范围:");
        dateRangeRow.createCell(1).setCellValue((startDate != null ? startDate : "全部") + " 至 " + (endDate != null ? endDate : "全部"));

        Row collegeRow = sheet.createRow(4);
        collegeRow.createCell(0).setCellValue("学院:");
        collegeRow.createCell(1).setCellValue(college != null ? college : "全部");

        Row dormitoryRow = sheet.createRow(5);
        dormitoryRow.createCell(0).setCellValue("宿舍楼:");
        dormitoryRow.createCell(1).setCellValue(dormitoryBuilding != null ? dormitoryBuilding : "全部");

        // A8: 趋势数据:
        Row trendDataTitleRow = sheet.createRow(7);
        Cell trendDataTitleCell = trendDataTitleRow.createCell(0);
        trendDataTitleCell.setCellValue("趋势数据:");
        trendDataTitleCell.setCellStyle(boldStyle);

        // 趋势数据表头 (A9:B9)
        Row trendHeaderRow = sheet.createRow(8);
        trendHeaderRow.createCell(0).setCellValue("星期");
        trendHeaderRow.createCell(1).setCellValue("晚归人数");
        trendHeaderRow.getCell(0).setCellStyle(boldStyle);
        trendHeaderRow.getCell(1).setCellStyle(boldStyle);

        // --- 趋势数据内容 (A10:B16) ---

        // 定义星期的顺序和中文名称
        String[] weekdaysEnglish = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String[] weekdaysChinese = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};

        // 使用 Map 存储数据，方便按星期查找
        Map<String, Integer> trendDataMap = new java.util.HashMap<>();
        if (weekLateReturnStatVOList != null) {
            for (WeekLateReturnStatVO stat : weekLateReturnStatVOList) {
                trendDataMap.put(stat.getWeekday(), stat.getLateReturnCount());
            }
        }

        // 填充周一到周日的数据
        for (int i = 0; i < weekdaysChinese.length; i++) {
            Row dataRow = sheet.createRow(9 + i); // 从第10行开始 (索引9)
            dataRow.createCell(0).setCellValue(weekdaysChinese[i]); // 写入中文星期

            // 获取对应的晚归人数，如果 Map 中没有则为 0
            Integer count = trendDataMap.getOrDefault(weekdaysEnglish[i], 0);
            dataRow.createCell(1).setCellValue(count); // 写入晚归人数
        }

        // --- 自动调整列宽（可选） ---
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    /**
     * 创建并填充 Sheet 3: 学院分布
     * @param workbook          Workbook 对象
     * @param collegeLateReturnStatVOList       学院分布数据 (List<CollegeLateReturnStatVO>)
     * @param startDate         筛选条件：开始日期
     * @param endDate           筛选条件：结束日期
     * @param college           筛选条件：学院
     * @param dormitoryBuilding 筛选条件：宿舍楼
     */
    private static void createCollegeSheet(
            Workbook workbook,
            List<CollegeLateReturnStatVO> collegeLateReturnStatVOList, // TODO: 替换为你的 CollegeLateReturnStatVO 列表类型
            String startDate,
            String endDate,
            String college,
            String dormitoryBuilding) {

        Sheet sheet = workbook.createSheet("学院分布"); // 创建 Sheet

        // --- 设置单元格样式（可选） ---
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        // --- 写入内容 ---

        // A1: 学院分布统计 (主标题)
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("学院分布统计");
        titleCell.setCellStyle(boldStyle);

        // A3: 筛选条件:
        Row filterTitleRow = sheet.createRow(2);
        Cell filterTitleCell = filterTitleRow.createCell(0);
        filterTitleCell.setCellValue("筛选条件:");
        filterTitleCell.setCellStyle(boldStyle);

        // 筛选条件详情 (A4:B6) - 同 Sheet 1 和 Sheet 2
        Row dateRangeRow = sheet.createRow(3);
        dateRangeRow.createCell(0).setCellValue("日期范围:");
        dateRangeRow.createCell(1).setCellValue((startDate != null ? startDate : "全部") + " 至 " + (endDate != null ? endDate : "全部"));

        Row collegeRow = sheet.createRow(4);
        collegeRow.createCell(0).setCellValue("学院:");
        collegeRow.createCell(1).setCellValue(college != null ? college : "全部");

        Row dormitoryRow = sheet.createRow(5);
        dormitoryRow.createCell(0).setCellValue("宿舍楼:");
        dormitoryRow.createCell(1).setCellValue(dormitoryBuilding != null ? dormitoryBuilding : "全部");

        // A8: 分布数据:
        Row collegeDataTitleRow = sheet.createRow(7);
        Cell collegeDataTitleCell = collegeDataTitleRow.createCell(0);
        collegeDataTitleCell.setCellValue("分布数据:");
        collegeDataTitleCell.setCellStyle(boldStyle);

        // 分布数据表头 (A9:C9)
        Row collegeHeaderRow = sheet.createRow(8);
        collegeHeaderRow.createCell(0).setCellValue("学院");
        collegeHeaderRow.createCell(1).setCellValue("晚归人数");
        collegeHeaderRow.createCell(2).setCellValue("占比 (%)");
        collegeHeaderRow.getCell(0).setCellStyle(boldStyle);
        collegeHeaderRow.getCell(1).setCellStyle(boldStyle);
        collegeHeaderRow.getCell(2).setCellStyle(boldStyle);

        // --- 分布数据内容 (从 A10 开始) ---
        if (collegeLateReturnStatVOList != null) {
            for (int i = 0; i < collegeLateReturnStatVOList.size(); i++) {
                CollegeLateReturnStatVO stat = collegeLateReturnStatVOList.get(i);
                Row dataRow = sheet.createRow(9 + i); // 从第10行开始 (索引9)
                dataRow.createCell(0).setCellValue(stat.getCollege()); // 写入学院名称
                dataRow.createCell(1).setCellValue(stat.getCount()); // 写入晚归人数
                dataRow.createCell(2).setCellValue(stat.getPercentage()); // 写入占比（假设是字符串，如 "XX.XX%"）
            }
        }

        // --- 自动调整列宽（可选） ---
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
    }

    /**
     * 创建并填充 Sheet 4: 宿舍楼统计
     * @param workbook          Workbook 对象
     * @param dormBuildingData  宿舍楼统计数据 (Map<String, List<DormitoryLateReturnStatVO>>)
     * @param startDate         筛选条件：开始日期
     * @param endDate           筛选条件：结束日期
     * @param college           筛选条件：学院
     * @param dormitoryBuilding 筛选条件：宿舍楼
     */
    private static void createDormBuildingSheet(
            Workbook workbook,
            Map<String, List<DormitoryLateReturnStatVO>> dormBuildingData, // TODO: 替换为你的 Service 实际返回的宿舍楼数据类型
            String startDate,
            String endDate,
            String college,
            String dormitoryBuilding) {

        Sheet sheet = workbook.createSheet("宿舍楼统计"); // 创建 Sheet

        // --- 设置单元格样式（可选） ---
        CellStyle boldStyle = workbook.createCellStyle();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldStyle.setFont(boldFont);

        // --- 写入内容 ---

        // A1: 宿舍楼晚归统计 (主标题)
        Row titleRow = sheet.createRow(0);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("宿舍楼晚归统计");
        titleCell.setCellStyle(boldStyle);

        // A3: 筛选条件:
        Row filterTitleRow = sheet.createRow(2);
        Cell filterTitleCell = filterTitleRow.createCell(0);
        filterTitleCell.setCellValue("筛选条件:");
        filterTitleCell.setCellStyle(boldStyle);

        // 筛选条件详情 (A4:B6) - 同其他 Sheet
        Row dateRangeRow = sheet.createRow(3);
        dateRangeRow.createCell(0).setCellValue("日期范围:");
        dateRangeRow.createCell(1).setCellValue((startDate != null ? startDate : "全部") + " 至 " + (endDate != null ? endDate : "全部"));

        Row collegeRow = sheet.createRow(4);
        collegeRow.createCell(0).setCellValue("学院:");
        collegeRow.createCell(1).setCellValue(college != null ? college : "全部");

        Row dormitoryRow = sheet.createRow(5);
        dormitoryRow.createCell(0).setCellValue("宿舍楼:");
        dormitoryRow.createCell(1).setCellValue(dormitoryBuilding != null ? dormitoryBuilding : "全部");

        // A8: 统计数据:
        Row dormStatsTitleRow = sheet.createRow(7);
        Cell dormStatsTitleCell = dormStatsTitleRow.createCell(0);
        dormStatsTitleCell.setCellValue("统计数据:");
        dormStatsTitleCell.setCellStyle(boldStyle);

        // 统计数据表头 (A9:B9)
        Row dormHeaderRow = sheet.createRow(8);
        dormHeaderRow.createCell(0).setCellValue("宿舍楼");
        dormHeaderRow.createCell(1).setCellValue("总晚归次数");
        dormHeaderRow.getCell(0).setCellStyle(boldStyle);
        dormHeaderRow.getCell(1).setCellStyle(boldStyle);

        // --- 统计数据内容 (从 A10 开始) ---
        // 从 Map 中获取 "building" 对应的 List
        List<DormitoryLateReturnStatVO> buildingList = null;
        if (dormBuildingData != null) {
            buildingList = dormBuildingData.get("building");
        }

        if (buildingList != null) {
            for (int i = 0; i < buildingList.size(); i++) {
                DormitoryLateReturnStatVO stat = buildingList.get(i);
                Row dataRow = sheet.createRow(9 + i); // 从第10行开始 (索引9)
                dataRow.createCell(0).setCellValue(stat.getDormitoryBuilding()); // 写入宿舍楼名称
                dataRow.createCell(1).setCellValue(stat.getTotalCountByBuilding()); // 写入总晚归次数
            }
        }

        // --- 自动调整列宽（可选） ---
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    // 处理晚归数据的导出
    public static void exportLateReturn(List<LateReturn> lateReturnList, ServletOutputStream outputStream)
            throws IOException {
        // 1. 创建工作簿和表
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("晚归记录");

        // 2. 标题行（中文）
        String[] titles = { "学号", "姓名", "学院", "宿舍号", "晚归时间", "晚归原因", "处理状态", "处理结果", "处理备注", "创建时间", "更新时间", "晚归记录ID" };
        Row titleRow = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
            titleRow.createCell(i).setCellValue(titles[i]);
        }

        // 3. 数据行
        int rowIdx = 2;
        for (LateReturn lr : lateReturnList) {
            Row row = sheet.createRow(rowIdx++);
            int col = 0;
            row.createCell(col++).setCellValue(nvl(lr.getStudentNo()));
            row.createCell(col++).setCellValue(nvl(lr.getStudentName()));
            row.createCell(col++).setCellValue(nvl(lr.getCollege()));
            row.createCell(col++).setCellValue(nvl(lr.getDormitory()));
            row.createCell(col++).setCellValue(formatDate(lr.getLateTime()));
            row.createCell(col++).setCellValue(nvl(lr.getReason()));
            row.createCell(col++).setCellValue(nvl(lr.getProcessStatus()));
            row.createCell(col++).setCellValue(nvl(lr.getProcessResult()));
            row.createCell(col++).setCellValue(nvl(lr.getProcessRemark()));
            row.createCell(col++).setCellValue(nvl(lr.getLateReturnId()));
        }

        // 自动列宽
        for (int i = 0; i < titles.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 5. 输出
        workbook.write(outputStream);
        workbook.close();
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String formatDate(Date date) {
        if (date == null)
            return "";
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }
}
