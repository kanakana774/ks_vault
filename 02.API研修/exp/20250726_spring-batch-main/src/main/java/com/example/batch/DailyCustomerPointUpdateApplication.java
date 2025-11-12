package com.example.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;


@SpringBootApplication
@EnableBatchProcessing // Spring Batch の自動設定を有効にする
public class DailyCustomerPointUpdateApplication {

    public static void main(String[] args) {
        SpringApplication.run(DailyCustomerPointUpdateApplication.class, args);
    }
}