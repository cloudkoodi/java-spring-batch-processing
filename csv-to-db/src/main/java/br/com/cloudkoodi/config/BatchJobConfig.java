package br.com.cloudkoodi.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchJobConfig {
    
    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    public BatchJobConfig(JobRepository jobRepository, PlatformTransactionManager platformTransactionManager) {
        this.jobRepository = jobRepository;
        this.platformTransactionManager = platformTransactionManager;
    }

    @Bean
    public Job job(Step step) {
        return new JobBuilder("user-processing-job", jobRepository)
                    .start(step)
                    .build();
    }

    @Bean
    public Step step() {
        return new StepBuilder("step-1", jobRepository)
                    .tasklet(new Tasklet() {

                        @Override
                        public RepeatStatus execute(StepContribution arg0, ChunkContext arg1) throws Exception {
                            System.out.println("Hello World");
                            return RepeatStatus.FINISHED;
                        }
                        
                    }, platformTransactionManager)
                .allowStartIfComplete(true)
                    .build();
    }
}
