package com.wwt.scdf;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.support.MessageBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Data;

@EnableBinding(Source.class)
@EnableAutoConfiguration
@SpringBootApplication
public class TimeSourceApplication {

	private static Logger logger = LoggerFactory.getLogger(TimeSourceApplication.class);
	private static ConcurrentHashMap<Long, String> cache = new ConcurrentHashMap<Long, String>();

	@Autowired
	RedisConnectionFactory redisConnection;

	@Bean(name = "redisServiceTemplate")
	public RedisTemplate<String, String> redisServiceTemplate() {
		RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
		redisTemplate.setConnectionFactory(redisConnection);
		return redisTemplate;
	}

	@SuppressWarnings("rawtypes")
	@Autowired
	@Qualifier("redisServiceTemplate")
	private RedisTemplate<String, String> redisTemplate;

	@Bean
	@InboundChannelAdapter(value = Source.OUTPUT, poller = @Poller(fixedDelay = "10000"))
	public MessageSource<String> message() throws IOException {
		logger.info("source invoked");
		return () -> MessageBuilder.withPayload(getErrorMessage()).build();
	}

	private String getErrorMessage() {
		logger.info("getErrorMessage() invoked ");
		String errorMessage = null;
		String paperTrailResponseEntity = readFromPaperTrail();
		logger.info("paperTrailResponseEntity====>" + paperTrailResponseEntity);
		errorMessage = extract(paperTrailResponseEntity);
		logger.info("pushing error Message from paper trails :: " + errorMessage);
		return errorMessage;
	}

	@Bean
	public CloseableHttpClient closeableHttpClient() {
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(new HttpHost("papertrailapp.com")),
				new UsernamePasswordCredentials("workflowevents@gmail.com", "Delta@123"));
		CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		return httpclient;
	}

	public String readFromPaperTrail() {

		String responseEntity = null;
		// HttpGet httpget = new HttpGet("https://papertrailapp.com/systems/loggregator/events?q=ERROR");
		HttpGet httpget = new HttpGet("https://papertrailapp.com/systems/wfevents/events?q=ERROR");
		logger.info("polling from papertrail :");
		httpget.setHeader("Accept", "application/json");
		// System.out.println("Executing request " + httpget.getRequestLine());
		CloseableHttpResponse response = null;
		try {
			response = closeableHttpClient().execute(httpget);
			responseEntity = EntityUtils.toString(response.getEntity());
		} catch (IOException e) {
			logger.error("error occured while reading the data from papertrail... " + e.getMessage());
		}

		return responseEntity;
	}

	public String extract(String jsonString) {
		JSONParser jsonParser = new JSONParser();
		JSONObject jsonObject = null;
		String msg = "NONE";
		String id = null;
		boolean isMsgAlreadyProcessed = false;
		try {

			if (jsonString != null && !jsonString.trim().equals("")) {

				jsonObject = (JSONObject) jsonParser.parse(jsonString);
				List<JSONObject> events = (List<JSONObject>) jsonObject.get("events");
				logger.info("events size " + events.size());
				if (events != null && events.size() > 0) {
					Iterator<JSONObject> eventsIterator = (Iterator<JSONObject>) events.iterator();
					while (eventsIterator.hasNext()) {
						JSONObject json = eventsIterator.next();
						String formattedMessage = (String) json.get("formatted_message");
						id = (String) json.get("id");
						logger.info("json message " + formattedMessage + " and it's id is :: " + id);
						String val = (String) redisTemplate.opsForHash().get("workflowevents", id);

						logger.info("id in redis is :: " + val);
						logger.info("redis size :: " + redisTemplate.opsForHash().size("workflowevents"));

						if (val == null || "".equals(val)) {
							redisTemplate.opsForHash().put("workflowevents", id, id);
							return msg = id;
						}
					}
				}
			}

			logger.info("end message is :: " + msg);

		} catch (ParseException e) {
//			e.printStackTrace();
			logger.error("parse error occured in parsing the json message from papertrail... ",e.getMessage());
		} catch (Exception e) {
//			e.printStackTrace();
			logger.error("exception occured in parsing the json message from papertrail... ",e.getMessage());
		}
		return msg;
	}

	public String extractErrorStringFromJson(String jsonString) {
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode objectNode = null;
		JsonNode node = null;
		try {
			objectNode = mapper.readValue(jsonString, ObjectNode.class);
			node = objectNode.get("events");
			String eventsArray = node.toString();
			Map<JsonNode, List> nodeArray = mapper.readValue(eventsArray, Map.class);
			System.out.println(nodeArray.size() - 1 + " ::::: " + nodeArray.size());
			// String lastNode=nodeArray.get(nodeArray.size()-1).toString();
			// JsonNode msg=lastNode.get("formatted_message");
			System.out.println(" msg " + nodeArray.toString());
			// String
			// errorMessage=lastNode.get("formatted_message").textValue();

			System.out.println("events node .. " + node.asText());
			// System.out.println("formatted message :: "+ errorMessage);
		} catch (IOException e) {
//			e.printStackTrace();
			logger.error("io error occured in extracting the json message ",e.getMessage());
		}
		return node.toString();
	}

	public static void main(String[] args) {
		SpringApplication.run(TimeSourceApplication.class, args);

		// ConcurrentHashMap<Long,String> concurrentHashMap=new
		// ConcurrentHashMap<Long,String>();
		// String val=concurrentHashMap.put(1L,"test");
		// val=concurrentHashMap.put(1L,"raj");
		// System.out.println("value put :: "+val);

		// System.out.println(concurrentHashMap.containsKey(1L));

	}

}

@Data
class Event {
	long id;
	String message;

}
