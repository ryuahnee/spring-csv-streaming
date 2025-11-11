package com.example.streaming.controller;

import com.example.streaming.dto.UserDto;
import com.example.streaming.repository.UserMapper;
import com.example.streaming.service.StreamingExcelService;
import com.example.streaming.util.ExcelUtil;
import com.example.streaming.util.MemoryMonitor;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequestMapping("/test")
@RestController
public class TestController {

    private final MemoryMonitor memoryMonitor;
    private final UserMapper mapper;
    private final StreamingExcelService streamingExcelService;

    public TestController(MemoryMonitor memoryMonitor, UserMapper mapper, StreamingExcelService streamingExcelService) {
        this.memoryMonitor = memoryMonitor;
        this.mapper = mapper;
        this.streamingExcelService = streamingExcelService;
    }

    @GetMapping("/payments/excel")
    public void downloadPaymentExcel(HttpServletResponse response) {
        // 위험한 코드 - 전체 데이터 로드
        List<UserDto> users = mapper.findAllUsers(); // 10만건!

        // Excel 생성
        ExcelUtil.createExcel(users, response);
    }


    @GetMapping("/memory")
    public String testMemory(){
        memoryMonitor.logMemoryStatus("테스트 시작");

        return String.format("현재 메모리: %dMB, 최대: %dMB, 사용률: %.2f%%",
                memoryMonitor.getCurrentMemoryUsageMB(),
                memoryMonitor.getMaxMemoryMB(),
                memoryMonitor.getMemoryUsagePercentage());
    }

    @GetMapping("/user")
    public void testUser(){
        List<UserDto> allUsers = mapper.findAllUsers();

        log.info("user건수 = {}" ,allUsers.size());
    }

    // ========== 블로그용 성능 비교 테스트 엔드포인트 ==========
    
    /**
     * 기존 방식: 전체 데이터 메모리 로드 + XSSFWorkbook
     * 예상: 10만건 이상에서 OOM 발생
     */
    @GetMapping("/excel/traditional")
    public void downloadTraditionalExcel(HttpServletResponse response) {
        memoryMonitor.logMemoryStatus("=== 기존 방식 Excel 생성 시작 ===");
        long startTime = System.currentTimeMillis();
        
        try {
            // 위험: 전체 데이터를 한번에 메모리에 로드
            List<UserDto> users = mapper.findAllUsers();
            log.info("전체 데이터 로드 완료: {}건", users.size());
            memoryMonitor.logMemoryStatus("전체 데이터 로드 후");

            // Excel 생성 (메모리 집약적)
            ExcelUtil.createExcel(users, response);
            
            long endTime = System.currentTimeMillis();
            log.info("=== 기존 방식 완료 - 처리시간: {}ms ===", (endTime - startTime));
            memoryMonitor.logMemoryStatus("=== 기존 방식 Excel 생성 완료 ===");

        } catch (OutOfMemoryError e) {
            log.error("  OOM 발생! 예상된 결과입니다.", e);
            throw new RuntimeException("메모리 부족으로 Excel 생성 실패 (예상된 결과)", e);
        } catch (Exception e) {
            log.error("기존 방식 Excel 생성 실패", e);
            throw new RuntimeException("Excel 생성 실패", e);
        }
    }

    /**
     * 스트리밍 방식: ResultHandler + SXSSFWorkbook
     * 예상: 100만건도 안정적 처리, 90% 메모리 절약
     */
    @GetMapping("/excel/streaming")
    public void downloadStreamingExcel(HttpServletResponse response) {
        memoryMonitor.logMemoryStatus("=== 스트리밍 방식 Excel 생성 시작 ===");
        long startTime = System.currentTimeMillis();
        
        try {
            // 스트리밍 서비스 호출
            streamingExcelService.createStreamingExcel(response);
            
            long endTime = System.currentTimeMillis();
            log.info("=== 스트리밍 방식 완료 - 처리시간: {}ms ===", (endTime - startTime));
            memoryMonitor.logMemoryStatus("=== 스트리밍 방식 Excel 생성 완료 ===");
            
        } catch (Exception e) {
            log.error("스트리밍 방식 Excel 생성 실패", e);
            throw new RuntimeException("스트리밍 Excel 생성 실패", e);
        }
    }

    /**
     * 메모리 사용량 실시간 모니터링
     * 브라우저에서 주기적으로 호출하여 메모리 상태 확인 가능
     */
    @GetMapping("/memory/status")
    public String getMemoryStatus() {
        memoryMonitor.logMemoryStatus("실시간 모니터링");
        
        return String.format("""
            {
                "currentMB": %d,
                "maxMB": %d,
                "usagePercent": %.2f,
                "timestamp": "%s"
            }
            """, 
            memoryMonitor.getCurrentMemoryUsageMB(),
            memoryMonitor.getMaxMemoryMB(),
            memoryMonitor.getMemoryUsagePercentage(),
            java.time.LocalDateTime.now()
        );
    }

    // ========== 데이터 준비용 엔드포인트 ==========
    
    /**
     * 테스트 데이터 생성 (100만건)
     * 충분한 메모리로 애플리케이션 실행해서 데이터 준비용
     */
    @GetMapping("/data/setup")
    public String setupTestData() {
        memoryMonitor.logMemoryStatus("대량 데이터 생성 시작");
        long startTime = System.currentTimeMillis();
        
        try {
            // H2 데이터베이스에 직접 대량 삽입
            mapper.createLargeDataset();
            
            long endTime = System.currentTimeMillis();
            int totalCount = mapper.countAllUsers();
            
            log.info("대량 데이터 생성 완료 - 총 {}건, 처리시간: {}ms", totalCount, (endTime - startTime));
            memoryMonitor.logMemoryStatus("대량 데이터 생성 완료");
            
            return String.format("테스트 데이터 생성 완료: %d건, 처리시간: %dms", totalCount, (endTime - startTime));
            
        } catch (Exception e) {
            log.error("테스트 데이터 생성 실패", e);
            return "데이터 생성 실패: " + e.getMessage();
        }
    }
}
