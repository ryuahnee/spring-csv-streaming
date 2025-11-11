package com.example.streaming.service;

import com.example.streaming.dto.UserDto;
import com.example.streaming.repository.UserMapper;
import com.example.streaming.util.MemoryMonitor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 메모리 효율적인 스트리밍 Excel 생성 서비스
 * 
 * OOM 문제 해결을 위해 다음 기술들을 적용:
 * 1. SXSSFWorkbook: 메모리에 일정 행수만 유지하고 나머지는 임시파일로 플러시
 * 2. MyBatis ResultHandler: DB에서 한 건씩 스트리밍 처리
 * 3. 실시간 메모리 모니터링: 처리 과정의 메모리 사용량 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingExcelService {

    /**
     * 메모리에 유지할 최대 행 수
     * 이 값을 초과하는 행들은 자동으로 임시 파일로 플러시됨
     */
    private static final int ROW_ACCESS_WINDOW_SIZE = 100;
    
    /**
     * 메모리 체크 주기 (건수 기준)
     */
    private static final int MEMORY_CHECK_INTERVAL = 10000;
    
    private final UserMapper userMapper;
    private final MemoryMonitor memoryMonitor;

    /**
     * 스트리밍 방식으로 Excel 파일 생성 및 응답
     * 
     * @param response HTTP 응답 객체
     */
    public void createStreamingExcel(HttpServletResponse response) {
        long startMB = memoryMonitor.getCurrentMemoryUsageMB();
        memoryMonitor.logMemoryStatus("스트리밍 Excel 생성 시작");
        
        SXSSFWorkbook workbook = null;
        
        try {
            // 1. 스트리밍 워크북 생성 (메모리 제한)
            workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW_SIZE);
            Sheet sheet = workbook.createSheet("사용자 목록");
            
            log.info("SXSSFWorkbook 생성 완료 - ROW_ACCESS_WINDOW_SIZE: {}", ROW_ACCESS_WINDOW_SIZE);
            
            // 2. 스타일 생성 (미리 생성해서 재사용)
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            
            // 3. 헤더 생성
            createHeader(sheet, headerStyle);
            
            // 4. 스트리밍 핸들러 생성
            StreamingResultHandler handler = new StreamingResultHandler(sheet, dataStyle);
            
            // 5. MyBatis ResultHandler로 데이터 스트리밍 처리
            log.info("데이터 스트리밍 처리 시작");
            userMapper.findAllUsersStreaming(handler);
            
            // 6. 응답 헤더 설정
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=users_streaming.xlsx");
            
            // 7. 파일 출력
            workbook.write(response.getOutputStream());
            
            long endMB = memoryMonitor.getCurrentMemoryUsageMB();
            log.info("스트리밍 Excel 생성 완료 - 시작: {}MB, 종료: {}MB, 총 처리건수: {}건", 
                    startMB, endMB, handler.getProcessedCount());
            
            memoryMonitor.logMemoryStatus("스트리밍 Excel 생성 완료");
            
        } catch (Exception e) {
            log.error("스트리밍 Excel 생성 중 오류 발생", e);
            throw new RuntimeException("스트리밍 Excel 생성 실패", e);
        } finally {
            // 8. 리소스 정리
            if (workbook != null) {
                try {
                    workbook.dispose(); // 임시 파일 정리
                    workbook.close();
                    log.debug("SXSSFWorkbook 리소스 정리 완료");
                } catch (IOException e) {
                    log.warn("워크북 정리 중 오류", e);
                }
            }
        }
    }

    /**
     * 헤더 행 생성
     */
    private void createHeader(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        
        String[] headers = {"ID", "사용자명", "이메일", "나이", "부서", "생성일시", "활성상태"};
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        log.debug("헤더 생성 완료 - 컬럼 수: {}", headers.length);
    }

    /**
     * 헤더 스타일 생성
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        
        // 헤더 폰트 설정
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        
        // 헤더 배경색 설정
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // 경계선 설정
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        return style;
    }

    /**
     * 데이터 스타일 생성
     */
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        // 경계선 설정
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        
        return style;
    }

    /**
     * 스트리밍용 ResultHandler
     * 
     * MyBatis가 DB에서 한 건씩 조회할 때마다 이 핸들러가 호출됨
     * 메모리에 전체 데이터를 로드하지 않고 건별로 처리
     */
    private class StreamingResultHandler implements ResultHandler<UserDto> {
        private final Sheet sheet;
        private final CellStyle dataStyle;
        private int currentRowNum = 1; // 헤더 다음 행부터 시작
        private int processedCount = 0;
        private long peakMemoryMB = 0;

        public StreamingResultHandler(Sheet sheet, CellStyle dataStyle) {
            this.sheet = sheet;
            this.dataStyle = dataStyle;
            this.peakMemoryMB = memoryMonitor.getCurrentMemoryUsageMB();
        }

        @Override
        public void handleResult(ResultContext<? extends UserDto> context) {
            try {
                UserDto user = context.getResultObject();
                
                // Excel 행 생성
                createDataRow(user);
                processedCount++;

                // 주기적 메모리 체크
                if (processedCount % MEMORY_CHECK_INTERVAL == 0) {
                    checkAndLogMemoryUsage();
                }

            } catch (Exception e) {
                log.error("사용자 데이터 처리 중 오류 발생 - 처리건수: {}", processedCount, e);
                throw new RuntimeException("Excel 데이터 처리 실패", e);
            }
        }

        /**
         * 사용자 데이터로 Excel 행 생성
         */
        private void createDataRow(UserDto user) {
            Row row = sheet.createRow(currentRowNum++);
            int col = 0;

            // ID
            Cell idCell = row.createCell(col++);
            idCell.setCellValue(user.getId());
            idCell.setCellStyle(dataStyle);

            // 사용자명
            Cell usernameCell = row.createCell(col++);
            usernameCell.setCellValue(user.getUsername());
            usernameCell.setCellStyle(dataStyle);

            // 이메일
            Cell emailCell = row.createCell(col++);
            emailCell.setCellValue(user.getEmail());
            emailCell.setCellStyle(dataStyle);

            // 나이
            Cell ageCell = row.createCell(col++);
            if (user.getAge() != null) {
                ageCell.setCellValue(user.getAge());
            }
            ageCell.setCellStyle(dataStyle);

            // 부서
            Cell deptCell = row.createCell(col++);
            deptCell.setCellValue(user.getDepartment());
            deptCell.setCellStyle(dataStyle);

            // 생성일시
            Cell createdAtCell = row.createCell(col++);
            if (user.getCreatedAt() != null) {
                createdAtCell.setCellValue(user.getCreatedAt().toString());
            }
            createdAtCell.setCellStyle(dataStyle);

            // 활성상태
            Cell activeCell = row.createCell(col++);
            activeCell.setCellValue(user.getActive() != null ? user.getActive().toString() : "");
            activeCell.setCellStyle(dataStyle);
        }

        /**
         * 메모리 사용량 체크 및 로깅
         */
        private void checkAndLogMemoryUsage() {
            long currentMB = memoryMonitor.getCurrentMemoryUsageMB();
            
            // 최고 메모리 사용량 업데이트
            if (currentMB > peakMemoryMB) {
                peakMemoryMB = currentMB;
            }

            log.info("스트리밍 진행: {}건 처리 - 현재 메모리: {}MB, 최고 메모리: {}MB", 
                    processedCount, currentMB, peakMemoryMB);

            // 메모리 사용률이 80% 초과시 경고
            double memoryUsage = memoryMonitor.getMemoryUsagePercentage();
            if (memoryUsage > 80.0) {
                log.warn("메모리 사용률 높음: {:.2f}% - 처리건수: {}", memoryUsage, processedCount);
                
                // 명시적 GC 수행 (개발/테스트 환경에서만)
                System.gc();
                log.info("명시적 GC 수행 완료");
            }
        }

        public int getProcessedCount() {
            return processedCount;
        }
    }
}