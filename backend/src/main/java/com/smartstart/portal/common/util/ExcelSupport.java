package com.smartstart.portal.common.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Shared workbook + styles for Excel exports. */
public class ExcelSupport implements AutoCloseable {

    public static final String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final XSSFWorkbook workbook = new XSSFWorkbook();
    public final CellStyle header;
    public final CellStyle money;
    public final CellStyle moneyBold;
    public final CellStyle bold;
    public final CellStyle centered;
    public final CellStyle weekend;
    public final CellStyle percent;

    public ExcelSupport() {
        DataFormat df = workbook.createDataFormat();
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        header = workbook.createCellStyle();
        header.setFont(boldFont);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setBorderBottom(BorderStyle.THIN);

        money = workbook.createCellStyle();
        money.setDataFormat(df.getFormat("$#,##0.00;-$#,##0.00"));

        moneyBold = workbook.createCellStyle();
        moneyBold.cloneStyleFrom(money);
        moneyBold.setFont(boldFont);

        bold = workbook.createCellStyle();
        bold.setFont(boldFont);

        centered = workbook.createCellStyle();
        centered.setAlignment(HorizontalAlignment.CENTER);

        weekend = workbook.createCellStyle();
        weekend.setAlignment(HorizontalAlignment.CENTER);
        weekend.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        weekend.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        percent = workbook.createCellStyle();
        percent.setDataFormat(df.getFormat("0.0%"));
    }

    public Workbook workbook() {
        return workbook;
    }

    public byte[] toBytes() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            workbook.close();
        } catch (IOException ignored) {
            // nothing to release
        }
    }
}
