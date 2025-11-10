package com.example.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.batch.domain.CustomerPointDto;
import com.example.batch.domain.PurchaseHistory;
import com.example.batch.item.CustomerPointProcessor;
import com.example.batch.item.CustomerPointWriter;
import com.example.batch.item.PurchaseHistoryItemReader;
import com.example.batch.tasklet.PromotionEmailSendingTasklet;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final PurchaseHistoryItemReader purchaseHistoryItemReader;
    private final CustomerPointProcessor customerPointProcessor;
    private final CustomerPointWriter customerPointWriter;
    private final PromotionEmailSendingTasklet promotionEmailSendingTasklet;

    /**
     * Step 1: 顧客ポイント更新処理 (Chunk指向)
     */
    @Bean
    public Step updateCustomerPointStep() {
        return new StepBuilder("updateCustomerPointStep", jobRepository)
                .<PurchaseHistory, CustomerPointDto>chunk(3, transactionManager)
                .reader(purchaseHistoryItemReader)
                .processor(customerPointProcessor)
                .writer(customerPointWriter)
                .build();
    }

    /**
     * Step 2: 特典メール配信処理 (Tasklet指向)
     */
    @Bean
    public Step sendPromotionEmailStep() {
        return new StepBuilder("sendPromotionEmailStep", jobRepository)
                .tasklet(promotionEmailSendingTasklet, transactionManager)
                .build();
    }

    /**
     * Job: 日次顧客ポイント更新＆特典メール配信バッチ
     */
    @Bean
    public Job dailyCustomerPointUpdateJob() {
        return new JobBuilder("dailyCustomerPointUpdateJob", jobRepository)
                .start(updateCustomerPointStep()) // まずポイント更新ステップを実行
                .next(sendPromotionEmailStep()) // 次にメール配信ステップを実行
                .build();
    }

}