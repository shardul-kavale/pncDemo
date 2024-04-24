package com.pnc.demo.service;

import jakarta.annotation.PostConstruct;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import com.ibm.gbs.schema.*;
import java.util.Properties;

@Service
public class CustomerBalanceService {
    @Autowired
    private StreamsBuilder streamsBuilder;
    @Autowired
    private Properties kafkaStreamsProperties;
    @Autowired
    private SpecificAvroSerde<Customer> customerSerde;
    @Autowired
    private SpecificAvroSerde<Balance> balanceSerde;
    @Autowired
    private SpecificAvroSerde<CustomerBalance> customerBalanceSerde;
    @Autowired
    private SpecificAvroSerde<NoMatchDLQ> noMatchDLQSerde;
    private KafkaStreams kafkaStreams;

    private static final Logger log = LoggerFactory.getLogger(CustomerBalanceService.class);

    @PostConstruct
    public void startStream() {
        //read Customer and reset the key to accountId
        KTable<String, Customer> customerTable = streamsBuilder.table("Customer", Consumed.with(Serdes.String(), customerSerde));
        KStream<String, Customer> transformedCustomerStream = customerTable.toStream().map((key, value) -> {
            log.info("Customer received - Key: {}, Value: {}", value.getAccountId().toString(), value);
            return KeyValue.pair(value.getAccountId().toString(), value);
        });
        KTable<String, Customer> transformedCustomerTable = transformedCustomerStream.toTable();

        //read Balance and reset the key to accountId
        KTable<String, Balance> balanceTable = streamsBuilder.table("Balance",  Consumed.with(Serdes.String(), balanceSerde));
        KStream<String, Balance> transformedBalanceStream = balanceTable.toStream().map((key, value) -> {
            log.info("Balance received - Key: {}, Value: {}", value.getAccountId().toString(), value);
            return KeyValue.pair(value.getAccountId().toString(), value);
        });
        KTable<String, Balance> transformedBalanceTable = transformedBalanceStream.toTable();

        KStream<String, CustomerBalance> joinedStream = transformedCustomerTable.leftJoin(transformedBalanceTable,
                (customer, balance) -> {
                    if (balance == null) {
                        log.error("No balance entry found for customer with Account ID: {}", customer.getAccountId());
                        return new CustomerBalance(customer.getAccountId(), customer.getCustomerId(), customer.getPhoneNumber(), -1.0f);
                    }
                    return new CustomerBalance(
                            customer.getAccountId(),
                            customer.getCustomerId(),
                            customer.getPhoneNumber(),
                            balance.getBalance());
                }).toStream();
        //Send to dead letter queue
        KStream<String, NoMatchDLQ> dlqStream = joinedStream
                .filter((key, value) -> value.getBalance() == -1.0f)
                .map((key, value) ->{
                    NoMatchDLQ dlqRecord = new NoMatchDLQ(value.getAccountId(), value.getCustomerId(),value.getPhoneNumber());
                    return new KeyValue<>(key, dlqRecord);
                });
        dlqStream.to("NoMatchDLQ", Produced.with(Serdes.String(), noMatchDLQSerde));
        dlqStream.peek((key, value) -> log.info("Sent to No Match DLQ :: key: {}, value: {}", key, value));

        // Continue with successful records
        KStream<String, CustomerBalance> successfulJoins = joinedStream.filter((key, value) -> value.getBalance() != -1.0f);
        successfulJoins.to("CustomerBalance", Produced.with(Serdes.String(), customerBalanceSerde));
        successfulJoins.peek((key, value) -> log.info("Sent to CustomerBalance :: key: {}, value: {}", key, value));

        kafkaStreams = new KafkaStreams(streamsBuilder.build(), kafkaStreamsProperties);
        kafkaStreams.start();
    }
}
