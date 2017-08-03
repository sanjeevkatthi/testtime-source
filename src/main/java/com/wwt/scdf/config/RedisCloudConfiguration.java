package com.wwt.scdf.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud")
public class RedisCloudConfiguration {
	
	private static Logger logger = LoggerFactory.getLogger(RedisCloudConfiguration.class);
	
	static{
		logger.info("In RedisCloudConfiguration =====>");
	}
	
	/*@Autowired
	RedisConnectionFactory redisConnection;
	
	@Bean(name = "redisServiceTemplate")
	public RedisTemplate<String, String> redisServiceTemplate() {
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
		redisTemplate.setConnectionFactory(redisConnection);
		return redisTemplate;
	}*/

}
