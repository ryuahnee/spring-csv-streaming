package com.example.streaming.controller;

import com.example.streaming.util.MemoryMonitor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/test")
@RestController
@RequiredArgsConstructor
public class TestController {

    private final MemoryMonitor memoryMonitor;

    @GetMapping("/memory")
    public String testMemory(){
        memoryMonitor.logMemoryStatus("테스트 시작");

        return String.format("현재 메모리: %dMB, 최대: %dMB, 사용률: %.2f%%",
                memoryMonitor.getCurrentMemoryUsageMB(),
                memoryMonitor.getMaxMemoryMB(),
                memoryMonitor.getMemoryUsagePercentage());
    }
}
