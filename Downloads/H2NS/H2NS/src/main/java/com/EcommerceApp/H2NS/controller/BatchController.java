package com.EcommerceApp.H2NS.controller;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job dailySalesReportJob;

    
    @Autowired
    public BatchController(JobLauncher jobLauncher, Job dailySalesReportJob) {
        this.jobLauncher = jobLauncher;
        this.dailySalesReportJob = dailySalesReportJob;
    }

    @GetMapping("/run")
    public ResponseEntity<String> runBatchJob() {
        try {
            
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();
            
            
            jobLauncher.run(dailySalesReportJob, params);
            
            return ResponseEntity.ok("Batch Job لحساب تقرير المبيعات اليومي بدأ بنجاح في الخلفية!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("فشل في تشغيل الـ Batch Job: " + e.getMessage());
        }
    }
}