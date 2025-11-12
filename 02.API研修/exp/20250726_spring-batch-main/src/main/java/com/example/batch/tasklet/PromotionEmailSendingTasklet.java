package com.example.batch.tasklet;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.example.batch.mapper.CustomerMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromotionEmailSendingTasklet implements Tasklet {

    private final CustomerMapper customerMapper;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // 例: ポイントランクが "GOLD" の顧客を抽出
        // List<CustomerPoint> goldCustomers =
        // customerMapper.findCustomersByRank("GOLD");

        // if (goldCustomers.isEmpty()) {

        // } else {
        // for (CustomerPoint customer : goldCustomers) {
        // System.out.println(customer.getCustomerId());
        // }
        // }
        return RepeatStatus.FINISHED;
    }

}