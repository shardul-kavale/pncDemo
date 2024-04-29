package com.pnc.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pnc.demo.event.SetupCompleteEvent;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.web.client.RestTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Configuration
public class KafkaSetupConfig {
    @Autowired
    private KafkaProperties kafkaProperties;
    @Autowired
    private RestTemplate restTemplate;
    @Value("${spring.kafka.streams.properties.schema.registry.url}")
    private String schemaRegistryUrl;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    private static final Logger log = LoggerFactory.getLogger(KafkaSetupConfig.class);
    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> adminProps = new HashMap<>();
        adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrapServers());
        adminProps.put(AdminClientConfig.CLIENT_ID_CONFIG, kafkaProperties.getAdmin().getClientId());
        return new KafkaAdmin(adminProps);
    }
    @Bean
    public AdminClient adminClient() {
        return AdminClient.create(kafkaAdmin().getConfigurationProperties());
    }
    @EventListener(ApplicationReadyEvent.class)
    public void setupKafkaTopicsAndSchemas() {
        try {
            createTopics();
        } catch (Exception e) {
            log.error("Failed to create topics: " + e.getMessage());
        }
        try {
            registerSchemas();
            eventPublisher.publishEvent(new SetupCompleteEvent(this));
        } catch (IOException e) {
            log.error("Failed to register Schemas: " + e.getMessage());
        }
    }
    private void createTopics() throws ExecutionException, InterruptedException {
        NewTopic newTopic1 = new NewTopic("Customer", 2, (short) 1);
        NewTopic newTopic2 = new NewTopic("Balance", 2, (short) 1);
        NewTopic newTopic3 = new NewTopic("CustomerBalance", 2, (short) 1);
        NewTopic newTopic4 = new NewTopic("NoMatchDLQ", 2, (short) 1);
        adminClient().createTopics(Arrays.asList(newTopic1, newTopic2, newTopic3, newTopic4)).all().get();
    }

    private void registerSchemas() throws IOException {
        Resource customer = new ClassPathResource("avro/customer.avsc");
        String customerSchema;
        try (InputStream is = customer.getInputStream()) {
            customerSchema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        Resource balance = new ClassPathResource("avro/balance.avsc");
        String balanceSchema;
        try (InputStream is = balance.getInputStream()) {
            balanceSchema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        Resource customerBalance = new ClassPathResource("avro/customerBalance.avsc");
        String customerBalSchema;
        try (InputStream is = customerBalance.getInputStream()) {
            customerBalSchema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        Resource noMatch = new ClassPathResource("avro/NoMatchDLQ.avsc");
        String noMatchSchema;
        try (InputStream is = noMatch.getInputStream()) {
            noMatchSchema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
        ObjectMapper mapper = new ObjectMapper();

        Map<String, String> customerPayload = new HashMap<>();
        customerPayload.put("schema", customerSchema);
        String customerResult = mapper.writeValueAsString(customerPayload);

        Map<String, String> balancePayload = new HashMap<>();
        balancePayload.put("schema", balanceSchema);
        String balanceResult = mapper.writeValueAsString(balancePayload);

        Map<String, String> customerBalPayload = new HashMap<>();
        customerBalPayload.put("schema", customerBalSchema);
        String customerBalanceResult = mapper.writeValueAsString(customerBalPayload);

        Map<String, String> noMatchPayload = new HashMap<>();
        noMatchPayload.put("schema", noMatchSchema);
        String noMatchResult = mapper.writeValueAsString(noMatchPayload);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity1 = new HttpEntity<>(customerResult, headers);
        HttpEntity<String> requestEntity2 = new HttpEntity<>(balanceResult, headers);
        HttpEntity<String> requestEntity3 = new HttpEntity<>(customerBalanceResult, headers);
        HttpEntity<String> requestEntity4 = new HttpEntity<>(noMatchResult, headers);

        restTemplate.postForLocation(schemaRegistryUrl + "/subjects/Customer-value/versions", requestEntity1);
        restTemplate.postForLocation(schemaRegistryUrl + "/subjects/Balance-value/versions", requestEntity2);
        restTemplate.postForLocation(schemaRegistryUrl + "/subjects/CustomerBalance-value/versions", requestEntity3);
        restTemplate.postForLocation(schemaRegistryUrl + "/subjects/NoMatchDLQ-value/versions", requestEntity4);


    }

}
