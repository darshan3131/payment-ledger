package com.darshan.payment_ledger.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

// WHY THIS EXISTS:
// Spring Boot 4 changed Kafka auto-configuration behaviour.
// KafkaTemplate is no longer auto-wired unless a ProducerFactory bean
// is explicitly registered. This mirrors what RedisConfig does for Redis.
// Without this, OutboxPoller fails to start with:
//   "No qualifying bean of type KafkaTemplate"

@Configuration
public class KafkaConfig {

    // Reads spring.kafka.bootstrap-servers from application.properties.
    // @Value binds a single property — safer than injecting the full
    // KafkaProperties object, which has too many moving parts in Boot 4.
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        // Where the Kafka broker lives (localhost:9092 by default).
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Serializers: Java String → byte[] for both key and value.
        // Must match what the consumer uses on the other end.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // acks=all: the producer waits for ALL in-sync replicas to confirm
        // the write before considering it successful.
        // For a payment system this is non-negotiable — no silent data loss.
        props.put(ProducerConfig.ACKS_CONFIG, "all");

        // Retries: if a transient network error happens, retry up to 3 times
        // before giving up and throwing an exception.
        props.put(ProducerConfig.RETRIES_CONFIG, 3);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        // KafkaTemplate wraps ProducerFactory.
        // OutboxPoller injects this bean by type: KafkaTemplate<String, String>.
        return new KafkaTemplate<>(producerFactory);
    }
}
