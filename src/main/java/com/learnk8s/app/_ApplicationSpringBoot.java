package com.learnk8s.app;

import com.learnk8s.app.queue.QueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;

@org.springframework.boot.autoconfigure.SpringBootApplication
@EnableJms
public class _ApplicationSpringBoot implements JmsListenerConfigurer {
    @Value("${queue.name}")
    private String queueName;

    @Value("${worker.name}")
    private String workerName;

    @Value("${worker.enabled}")
    private boolean workerEnabled;

    @Autowired
    private QueueService queueService;

	public static void main(String[] args) {
        SpringApplication.run(_ApplicationSpringBoot.class, args);
        System.out.println("http://localhost:8080");
        System.out.println("http://localhost:8080/health");
        System.out.println("http://localhost:8080/health2");
        System.out.println("http://localhost:8080/submit/10");
        System.out.println("http://localhost:8080/metrics");
	}

    @Override
    public void configureJmsListeners(JmsListenerEndpointRegistrar registrar) {
	    if (workerEnabled) {
            SimpleJmsListenerEndpoint endpoint = new SimpleJmsListenerEndpoint();
            endpoint.setId(workerName);
            endpoint.setDestination(queueName);
            endpoint.setMessageListener(queueService);
            registrar.registerEndpoint(endpoint);

        }
    }
}
