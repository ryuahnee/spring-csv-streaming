package com.example.streaming.util;

import com.example.streaming.dto.UserDto;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.util.List;

public class ExcelUtil {

    public static void createExcelWithSXSSF(List<UserDto> users, HttpServletResponse response) {

        SXSSFWorkbook workbook = null;

        try {
            // 메모리 제한: 100 rows만 메모리에 유지
            workbook = new SXSSFWorkbook(100);
            Sheet sheet = workbook.createSheet("사용자 목록");

            // Header Row
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("ID");
            headerRow.createCell(1).setCellValue("이름");
            headerRow.createCell(2).setCellValue("이메일");

            // List<UserDto> 전체를 반복하면서 SXSSF 로 기입
            for (int i = 0; i < users.size(); i++) {
                UserDto user = users.get(i);
                Row row = sheet.createRow(i + 1);

                row.createCell(0).setCellValue(user.getId());
                row.createCell(1).setCellValue(user.getUsername());
                row.createCell(2).setCellValue(user.getEmail());
            }

            // Response 설정
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=users_sxssf.xlsx");

            workbook.write(response.getOutputStream());

        } catch (IOException e) {
            throw new RuntimeException("SXSSF Excel 생성 실패", e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.dispose(); // temp 파일 삭제
                    workbook.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
