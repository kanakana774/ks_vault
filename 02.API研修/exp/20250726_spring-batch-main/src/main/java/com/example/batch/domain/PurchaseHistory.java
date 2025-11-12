package com.example.batch.domain;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PurchaseHistory {
    private Long id;
    private String customerId;
    private LocalDate purchaseDate;
    private String itemName;
    private Integer amount;
    private LocalDateTime createdAt;
}