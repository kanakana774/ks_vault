package com.example.demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class JobStartupRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job sampleJob;

    public JobStartupRunner(JobLauncher jobLauncher, Job sampleJob) {
        this.jobLauncher = jobLauncher;
        this.sampleJob = sampleJob;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        executeJob();
    }

    private void executeJob() throws JobExecutionException {
        // ユニークなJobParametersを生成
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("time", System.currentTimeMillis()).toJobParameters();

        // ジョブを実行
        jobLauncher.run(sampleJob, jobParameters);

    }
}
