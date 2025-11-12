package com.example.batch.domain;

import java.time.YearMonth;

import lombok.Data;

@Data
public class AggregatedPointDto {
    private String customerId;
    private YearMonth month;
    private Integer totalPoints;
}
