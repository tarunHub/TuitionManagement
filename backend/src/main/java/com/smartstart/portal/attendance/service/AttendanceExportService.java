package com.smartstart.portal.attendance.service;

import com.smartstart.portal.attendance.dto.AttendanceDtos.AttendanceGridResponse;
import com.smartstart.portal.attendance.dto.AttendanceDtos.GridDay;
import com.smartstart.portal.attendance.dto.AttendanceDtos.GridRow;
import com.smartstart.portal.attendance.entity.AttendanceStatus;
import com.smartstart.portal.common.util.AppTime;
import com.smartstart.portal.common.util.ExcelSupport;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Excel export mirroring the on-screen monthly grid. */
@Service
@RequiredArgsConstructor
public class AttendanceExportService {

    private final AttendanceService attendanceService;

    public byte[] export(YearMonth month, Long groupId) {
        AttendanceGridResponse grid = attendanceService.grid(month, groupId);
        List<GridDay> days = grid.days();
        try (ExcelSupport xl = new ExcelSupport()) {
            Sheet sheet = xl.workbook().createSheet("Attendance " + month);
            int fixed = 2;
            int totalsCol = fixed + days.size();

            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("Attendance — " + AppTime.monthLabel(month));
            title.getCell(0).setCellStyle(xl.bold);

            Row letters = sheet.createRow(1);
            Row numbers = sheet.createRow(2);
            header(letters, 0, "", xl);
            header(letters, 1, "", xl);
            header(numbers, 0, "Student ID", xl);
            header(numbers, 1, "Name", xl);
            for (int i = 0; i < days.size(); i++) {
                header(letters, fixed + i, days.get(i).dayLetter(), xl);
                header(numbers, fixed + i, String.valueOf(days.get(i).day()), xl);
            }
            String[] totals = {"Present", "Absent", "Held", "Attendance %"};
            for (int i = 0; i < totals.length; i++) {
                header(letters, totalsCol + i, "", xl);
                header(numbers, totalsCol + i, totals[i], xl);
            }

            int r = 3;
            for (GridRow row : grid.rows()) {
                Row x = sheet.createRow(r++);
                x.createCell(0).setCellValue(row.studentCode());
                x.createCell(1).setCellValue(row.fullName());
                for (int i = 0; i < days.size(); i++) {
                    Cell c = x.createCell(fixed + i);
                    AttendanceStatus s = row.cells().get(i);
                    if (s != null) {
                        c.setCellValue(s == AttendanceStatus.PRESENT ? "P" : "A");
                    }
                    c.setCellStyle(days.get(i).weekend() ? xl.weekend : xl.centered);
                }
                x.createCell(totalsCol).setCellValue(row.present());
                x.createCell(totalsCol + 1).setCellValue(row.absent());
                x.createCell(totalsCol + 2).setCellValue(row.held());
                setPct(x.createCell(totalsCol + 3), row.attendancePct(), xl);
            }

            Row footer = sheet.createRow(r);
            footer.createCell(0).setCellValue("Present per day");
            footer.getCell(0).setCellStyle(xl.bold);
            sheet.addMergedRegion(new CellRangeAddress(r, r, 0, 1));
            for (int i = 0; i < days.size(); i++) {
                Cell c = footer.createCell(fixed + i);
                c.setCellValue(grid.presentPerDay().get(i));
                c.setCellStyle(xl.centered);
            }
            footer.createCell(totalsCol).setCellValue(grid.totals().present());
            footer.createCell(totalsCol + 1).setCellValue(grid.totals().absent());
            footer.createCell(totalsCol + 2).setCellValue(grid.totals().held());
            setPct(footer.createCell(totalsCol + 3), grid.totals().attendancePct(), xl);

            sheet.setColumnWidth(0, 12 * 256);
            sheet.setColumnWidth(1, 24 * 256);
            for (int i = 0; i < days.size(); i++) {
                sheet.setColumnWidth(fixed + i, 4 * 256);
            }
            for (int i = 0; i < totals.length; i++) {
                sheet.setColumnWidth(totalsCol + i, 13 * 256);
            }
            sheet.createFreezePane(fixed, 3);
            return xl.toBytes();
        }
    }

    private static void header(Row row, int col, String value, ExcelSupport xl) {
        Cell c = row.createCell(col);
        c.setCellValue(value);
        c.setCellStyle(xl.header);
    }

    private static void setPct(Cell cell, BigDecimal pct, ExcelSupport xl) {
        if (pct != null) {
            cell.setCellValue(pct.doubleValue() / 100.0);
            cell.setCellStyle(xl.percent);
        } else {
            cell.setCellValue("—");
        }
    }
}
