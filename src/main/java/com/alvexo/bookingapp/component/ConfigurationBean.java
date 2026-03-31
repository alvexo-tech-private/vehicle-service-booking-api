package com.alvexo.bookingapp.component;
import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ConfigurationBean {
	
	@Bean(name = "taskExecutor")
	public Executor taskExecutor() {
	    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
	    executor.setCorePoolSize(5);
	    executor.setMaxPoolSize(10);
	    executor.setQueueCapacity(100);
	    executor.setThreadNamePrefix("Async-");
	    executor.initialize();
	    return executor;
	}
}

