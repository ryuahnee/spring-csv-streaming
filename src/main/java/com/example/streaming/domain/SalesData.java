package com.example.streaming.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 판매 데이터 엔티티
 * 대용량 엑셀 다운로드 테스트를 위한 샘플 데이터 모델
 */

@Entity
@Table(name = "sales_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesData {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /**
         * 거래 ID
         */
        @Column(name = "transaction_id", nullable = false, length = 50)
        private String transactionId;

        /**
         * 판매자 ID
         */
        @Column(name = "seller_id", nullable = false, length = 20)
        private String sellerId;

        /**
         * 판매자명
         */
        @Column(name = "seller_name", nullable = false, length = 100)
        private String sellerName;

        /**
         * 상품명
         */
        @Column(name = "product_name", nullable = false, length = 200)
        private String productName;

        /**
         * 판매 금액
         */
        @Column(name = "amount", nullable = false, precision = 15, scale = 2)
        private BigDecimal amount;

        /**
         * 수수료
         */
        @Column(name = "fee", nullable = false, precision = 15, scale = 2)
        private BigDecimal fee;

        /**
         * 수수료율 (%)
         */
        @Column(name = "fee_rate", nullable = false, precision = 5, scale = 3)
        private BigDecimal feeRate;

        /**
         * 거래 일시
         */
        @Column(name = "transaction_date", nullable = false)
        private LocalDateTime transactionDate;

        /**
         * 결제 방법
         */
        @Column(name = "payment_method", length = 50)
        private String paymentMethod;

        /**
         * 거래 상태
         */
        @Column(name = "status", length = 20)
        private String status;

        /**
         * 등록일시
         */
        @Column(name = "created_at", nullable = false)
        private LocalDateTime createdAt;

        /**
         * 수정일시
         */
        @Column(name = "updated_at")
        private LocalDateTime updatedAt;

        @PrePersist
        protected void onCreate() {
            createdAt = LocalDateTime.now();
            updatedAt = LocalDateTime.now();
        }

        @PreUpdate
        protected void onUpdate() {
            updatedAt = LocalDateTime.now();
        }
}
