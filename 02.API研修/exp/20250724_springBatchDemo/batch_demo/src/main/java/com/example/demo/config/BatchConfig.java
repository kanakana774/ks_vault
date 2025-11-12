package com.example.demo.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.batch.MyBatisCursorItemReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import com.example.demo.entity.GameHistory;
import com.example.demo.entity.TScore;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    // Stepの定義
    @Bean
    public Step step1(JobRepository jobRepository,
            DataSourceTransactionManager transactionManager,
            ItemReader<TScore> reader,
            ItemProcessor<TScore, GameHistory> processor,
            ItemWriter<GameHistory> writer) {
        return new StepBuilder("step1", jobRepository)
                .<TScore, GameHistory>chunk(3, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    // Jobの定義
    @Bean
    public Job job(JobRepository jobRepository, Step step1) {
        return new JobBuilder("sampleJob", jobRepository)
                .start(step1)
                .build();
    }

    // reader定義
    @Bean
    @StepScope
    public ItemReader<TScore> myBatisCursorItemReader(SqlSessionFactory sqlSessionFactory) {
        MyBatisCursorItemReader<TScore> reader = new MyBatisCursorItemReader<>();
        reader.setSqlSessionFactory(sqlSessionFactory);
        reader.setQueryId("com.example.demo.mapper.TScoreMapper.selectAll");
        return reader;
    }
}
