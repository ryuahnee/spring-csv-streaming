package com.example.streaming.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정보 DTO
 * MyBatis ResultHandler 테스트를 위한 사용자 데이터 모델
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    /**
     * 사용자 ID
     */
    private Long id;

    /**
     * 사용자명
     */
    private String username;

    /**
     * 이메일
     */
    private String email;

    /**
     * 나이
     */
    private Integer age;

    /**
     * 부서
     */
    private String department;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 활성 상태
     */
    private Boolean active;
}