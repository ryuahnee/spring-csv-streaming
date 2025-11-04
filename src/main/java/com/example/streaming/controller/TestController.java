package com.example.streaming.controller;

import com.example.streaming.dto.UserDto;
import com.example.streaming.repository.UserMapper;
import com.example.streaming.util.MemoryMonitor;
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

    public TestController(MemoryMonitor memoryMonitor, UserMapper mapper) {
        this.memoryMonitor = memoryMonitor;
        this.mapper = mapper;
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
}
