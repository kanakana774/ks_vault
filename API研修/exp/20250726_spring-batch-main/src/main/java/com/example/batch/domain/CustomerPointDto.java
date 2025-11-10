package com.example.batch.domain;

import java.time.YearMonth;

import lombok.Data;

@Data
public class CustomerPointDto {
    private String customerId;
    private YearMonth pointsAddedMonth;
    private Integer addedPoints;
}