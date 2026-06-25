package com.EcommerceApp.H2NS.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.EcommerceApp.H2NS.loadbalancer.OrderLoadDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class TestLoadBalancerController {

    private static final AtomicInteger counter = new AtomicInteger(0);

    private static final int[] PORTS = { 8081, 8082, 8083 };
    @Autowired
    private OrderLoadDistributor orderLoadDistributor;

    @GetMapping("/test-instance")
    public Map<String, Object> testInstance() {

        Map<String, Object> response = new HashMap<>();

        String currentPort = System.getProperty("server.port", "unknown");

        int index = counter.getAndIncrement() % PORTS.length;
        int selectedPort = PORTS[index];

        response.put("status", "SUCCESS");
        response.put("current_server_port", currentPort);
        response.put("load_balanced_to_port", selectedPort);
        response.put("message", "Request would be routed to: localhost:" + selectedPort);
        response.put("round_robin_counter", counter.get());

        return response;
    }

    @GetMapping("/test")
    public String test() {
        return "Next port: " + orderLoadDistributor.getNextPort();
    }
}