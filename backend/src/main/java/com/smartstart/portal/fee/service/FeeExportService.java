package com.smartstart.portal.fee.service;

import com.smartstart.portal.common.util.AppTime;
import com.smartstart.portal.common.util.ExcelSupport;
import com.smartstart.portal.fee.dto.FeeDtos.FeeListResponse;
import com.smartstart.portal.fee.dto.FeeDtos.FeeRowResponse;
import com.smartstart.portal.fee.entity.FeeStatus;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class FeeExportService {

    private static final String[] HEADERS = {"Student ID", "Name", "Grade", "Rate ($/class)", "Classes Held",
            "Classes Scheduled (billed)", "Amount Due", "Amount Paid", "Balance", "Status", "Last Payment Date", "Method", "Notes"};

    private final FeeService feeService;

    public byte[] export(YearMonth month, FeeStatus status, Long groupId) {
        FeeListResponse fees = feeService.list(month, status, groupId);
        try (ExcelSupport xl = new ExcelSupport()) {
            Sheet sheet = xl.workbook().createSheet("Fees " + month);
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Monthly fees — " + AppTime.monthLabel(month));
            title.getCell(0).setCellStyle(xl.bold);

            Row header = sheet.createRow(1);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(HEADERS[i]);
                c.setCellStyle(xl.header);
            }

            int r = 2;
            for (FeeRowResponse row : fees.rows()) {
                Row x = sheet.createRow(r++);
                x.createCell(0).setCellValue(row.studentCode());
                x.createCell(1).setCellValue(row.fullName());
                x.createCell(2).setCellValue(row.grade());
                money(x, 3, row.rate(), xl, false);
                x.createCell(4).setCellValue(row.classesHeld());
                if (row.classesScheduled() != null) {
                    x.createCell(5).setCellValue(row.classesScheduled());
                }
                money(x, 6, row.amountDue(), xl, false);
                money(x, 7, row.amountPaid(), xl, false);
                money(x, 8, row.balance(), xl, false);
                x.createCell(9).setCellValue(row.status().name());
                x.createCell(10).setCellValue(row.lastPaymentDate() == null ? "" : row.lastPaymentDate().toString());
                x.createCell(11).setCellValue(row.lastPaymentMethod() == null ? "" : row.lastPaymentMethod().name());
                x.createCell(12).setCellValue(row.notes() == null ? "" : row.notes());
            }

            Row total = sheet.createRow(r);
            total.createCell(0).setCellValue("TOTAL");
            total.getCell(0).setCellStyle(xl.bold);
            money(total, 6, fees.totals().amountDue(), xl, true);
            money(total, 7, fees.totals().amountPaid(), xl, true);
            money(total, 8, fees.totals().balance(), xl, true);

            int[] widths = {12, 24, 10, 14, 20, 18, 13, 13, 13, 10, 18, 12, 24};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(2, 2);
            return xl.toBytes();
        }
    }

    private static void money(Row row, int col, BigDecimal value, ExcelSupport xl, boolean bold) {
        Cell c = row.createCell(col);
        c.setCellValue(value == null ? 0 : value.doubleValue());
        c.setCellStyle(bold ? xl.moneyBold : xl.money);
    }
}
