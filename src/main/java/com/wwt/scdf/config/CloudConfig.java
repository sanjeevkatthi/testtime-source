package com.wwt.scdf.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.cloud.service.common.AmqpServiceInfo;
import org.springframework.cloud.service.common.RedisServiceInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
@Profile("cloud")
public class CloudConfig {
	
	private static Logger logger = LoggerFactory.getLogger(CloudConfig.class);
	
	@Bean
	public Cloud cloud() {
		CloudFactory cloudFactory = new CloudFactory();
		return cloudFactory.getCloud();
	}

	@Bean
	public RedisConnectionFactory redisConnection() {
		logger.info("invoking redis service=====>");
		RedisServiceInfo serviceInfo = (RedisServiceInfo) cloud().getServiceInfo("redis");
		String serviceID = serviceInfo.getId();
		logger.info("serviceInfo.getHost()=====>"+serviceInfo.getHost());
		logger.info("serviceInfo.getPort()=====>"+serviceInfo.getPort());
		logger.info("serviceInfo.getPassword()====>"+serviceInfo.getPassword());
		return cloud().getServiceConnector(serviceID, RedisConnectionFactory.class, null);
	}
	
	@Bean
	public ConnectionFactory connectionFactory() {
		AmqpServiceInfo amqpServiceInfo = (AmqpServiceInfo) cloud().getServiceInfo("rabbitmq");
		String serviceId = amqpServiceInfo.getId();
		return cloud().getServiceConnector(serviceId, ConnectionFactory.class, null);
	}

}
