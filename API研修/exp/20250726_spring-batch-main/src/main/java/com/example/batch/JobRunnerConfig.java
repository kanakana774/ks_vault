package com.example.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class JobRunnerConfig {
    @Bean
    public ApplicationRunner runJob(JobLauncher jobLauncher,
            @Qualifier("dailyCustomerPointUpdateJob") Job dailyCustomerPointUpdateJob) {
        return args -> {
            try {
                // ユニークなJobParametersを生成
                JobParameters jobParameters = new JobParametersBuilder()
                        .addLong("time", System.currentTimeMillis()).toJobParameters();
                // ジョブを実行
                jobLauncher.run(dailyCustomerPointUpdateJob, jobParameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }
}