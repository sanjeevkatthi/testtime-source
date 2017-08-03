package com.wwt.scdf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

@Configuration
@Profile("default")
public class RedisConfiguration {

	@Bean
	public RedisConnectionFactory redisConnection() {
		return new JedisConnectionFactory();
	}

}
