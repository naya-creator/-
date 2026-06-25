package com.EcommerceApp.H2NS.loadbalancer;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class OrderLoadDistributor {

    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final int[] PORTS = { 8081, 8082, 8083 };

    public int getNextPort() {
        return PORTS[counter.getAndIncrement() % PORTS.length];
    }

    public String getNextUrl() {
        int port = getNextPort();
        return "http://localhost:" + port;
    }
}