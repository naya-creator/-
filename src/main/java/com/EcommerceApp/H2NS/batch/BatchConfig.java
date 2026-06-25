package com.EcommerceApp.H2NS.batch;

import com.EcommerceApp.H2NS.model.Order;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
// import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
// import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    public BatchConfig(JobRepository jobRepository, 
                       PlatformTransactionManager transactionManager, 
                       EntityManagerFactory entityManagerFactory) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Bean
    public JpaPagingItemReader<Order> orderReader() {
        return new JpaPagingItemReaderBuilder<Order>()
                .name("orderReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT o FROM Order o WHERE o.status = :status")
                .parameterValues(Map.of("status", Order.OrderStatus.CONFIRMED))
                .pageSize(500)
                .build();
    }

    @Bean
    public ItemProcessor<Order, Order> orderProcessor() {
        return order -> order; 
    }

    @Bean(name = "batchTaskExecutor")
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); 
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Batch-Worker-");
        executor.initialize();
        return executor;
    }

   
    @Bean
    public Step auditStep(SalesReportWriter salesReportWriter) { 
        return new StepBuilder("auditStep", jobRepository)
                .<Order, Order>chunk(500, transactionManager) 
                .reader(orderReader())
                .processor(orderProcessor())
                .writer(salesReportWriter) 
                .taskExecutor(batchTaskExecutor()) 
                .build();
    }
    @Bean
    public Job dailySalesReportJob(Step auditStep) {
        return new JobBuilder("dailySalesReportJob", jobRepository)
                .start(auditStep)
                .build();
    }
}