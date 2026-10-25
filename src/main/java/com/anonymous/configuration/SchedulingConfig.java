package com.anonymous.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// Background jobs (chatbot history retention)
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
