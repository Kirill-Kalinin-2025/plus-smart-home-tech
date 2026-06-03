package ru.yandex.practicum.analyzer.config;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import ru.yandex.practicum.analyzer.kafka.deserializer.HubEventDeserializer;
import ru.yandex.practicum.analyzer.kafka.deserializer.SensorsSnapshotDeserializer;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final Environment environment;

    @Bean
    public KafkaConsumer<String, SpecificRecordBase> snapshotConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty("spring.kafka.bootstrap-servers"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                SensorsSnapshotDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                environment.getProperty("analyzer.kafka.consumer.snapshots.group-id"));
        props.put(ConsumerConfig.CLIENT_ID_CONFIG,
                environment.getProperty("analyzer.kafka.consumer.snapshots.client-id"));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                environment.getProperty("analyzer.kafka.consumer.snapshots.enable-auto-commit"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(props);
    }

    @Bean
    public KafkaConsumer<String, SpecificRecordBase> hubConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                environment.getProperty("spring.kafka.bootstrap-servers"));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                HubEventDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                environment.getProperty("analyzer.kafka.consumer.hubs.group-id"));
        props.put(ConsumerConfig.CLIENT_ID_CONFIG,
                environment.getProperty("analyzer.kafka.consumer.hubs.client-id"));
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,
                environment.getProperty("analyzer.kafka.consumer.hubs.enable-auto-commit"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new KafkaConsumer<>(props);
    }
}