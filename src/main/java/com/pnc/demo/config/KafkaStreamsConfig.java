package com.pnc.demo.config;

import com.ibm.gbs.schema.*;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Configuration
public class KafkaStreamsConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    @Value("${spring.kafka.streams.application-id}")
    private String applicationId;
    @Value("${spring.kafka.streams.properties.schema.registry.url}")
    private String schemaRegistryUrl;
    @Bean
    public Properties kafkaStreamsProperties() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(StreamsConfig.PROCESSING_GUARANTEE_CONFIG, StreamsConfig.EXACTLY_ONCE_V2);
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/var/lib/kafka-streams");
        props.put("schema.registry.url", schemaRegistryUrl);
        return props;
    }
    @Bean
    public StreamsBuilder streamsBuilder() {
        return new StreamsBuilder();
    }
    @Bean
    public Map<String, Object> serdeConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", true);
        return props;
    }
    @Bean
    public SpecificAvroSerde<Customer> customerSerde() {
        SpecificAvroSerde<Customer> serde = new SpecificAvroSerde<>();
        serde.configure(serdeConfig(), false);
        return serde;
    }
    @Bean
    public SpecificAvroSerde<Balance> balanceSerde() {
        SpecificAvroSerde<Balance> serde = new SpecificAvroSerde<>();
        serde.configure(serdeConfig(), false);
        return serde;
    }
    @Bean
    public SpecificAvroSerde<CustomerBalance> customerBalanceSerde() {
        SpecificAvroSerde<CustomerBalance> serde = new SpecificAvroSerde<>();
        serde.configure(serdeConfig(), false);
        return serde;
    }
    @Bean
    public SpecificAvroSerde<NoMatchDLQ> noMatchDLQSerde() {
        SpecificAvroSerde<NoMatchDLQ> serde = new SpecificAvroSerde<>();
        serde.configure(serdeConfig(), false);
        return serde;
    }
}
