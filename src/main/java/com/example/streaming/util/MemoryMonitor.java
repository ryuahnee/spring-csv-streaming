package com.example.streaming.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 메모리 사용량 모니터링 유틸리티
 *
 * 대용량 엑셀 처리 시 메모리 사용량을 실시간으로 모니터링하여
 * 성능 분석 및 최적화 포인트를 찾기 위한 도구
 */
@Slf4j
@Component
public class MemoryMonitor {
    /**
     * 현재 메모리 사용량을 MB 단위로 반환
     */
    public long getCurrentMemoryUsageMB() {
        // Runtime 객체 가져오기
        Runtime runtime = Runtime.getRuntime();

        // 사용중인 메모리 = 전체 할당된 메모리 - 여유 메모리
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        // 바이트 → MB 변환 (1024 * 1024로 나누기)
        return usedMemory / (1024 * 1024);
    }

    /**
     * 최대 가용 메모리를 MB 단위로 반환
     */
    public long getMaxMemoryMB() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        return maxMemory / (1024 * 1024);
    }

    /**
     * 메모리 사용률을 퍼센트로 반환
     */
    public double getMemoryUsagePercentage() {
        Runtime runtime = Runtime.getRuntime();

        // 사용중인 메모리 계산
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        // 사용률 = (사용메모리 / 최대메모리) * 100
        return (double) usedMemory / maxMemory * 100;
    }

    /**
     * 현재 메모리 상태를 로그로 출력
     */
    public void logMemoryStatus(String context) {
        long current = getCurrentMemoryUsageMB();
        long max = getMaxMemoryMB();
        double percentage = getMemoryUsagePercentage();

        String message = String.format("메모리 상태 [%s] - 사용: %dMB, 최대: %dMB, 사용률: %.2f%%",
                context, current, max, percentage);
        log.info(message);
    }

}
