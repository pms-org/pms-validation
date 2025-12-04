package com.pms.validation.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.proto.TradeEventProto;

@Configuration
public class KafkaConfig {

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put("bootstrap.servers", "localhost:9092");
        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic validationTopic() {
        return TopicBuilder.name("validation-topic")
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ingestionTopic() {
        return TopicBuilder.name("ingestion-topic")
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic invalidTradeTopic() {
        return TopicBuilder.name("invalid-trade-topic")
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, TradeEventProto> producerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // IMPORTANT: Use Protobuf serializer
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class);

        // REQUIRED for Protobuf
        props.put("schema.registry.url", "http://localhost:8081");

        // Optional but recommended
        props.put("use.latest.version", true);
        props.put("auto.register.schemas", true);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, TradeEventProto> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean(name = "protobufKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, TradeEventProto> protobufKafkaListenerContainerFactory() {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "pms-core-consumer-group");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // IMPORTANT: Protobuf deserializer
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer.class);

        // REQUIRED
        props.put("schema.registry.url", "http://localhost:8081");

        // IMPORTANT: Tell Kafka which Protobuf type to convert the bytes into
        props.put("specific.protobuf.value.type",
                "com.pms.validation.proto.TradeEventProto");

        DefaultKafkaConsumerFactory<String, TradeEventProto> consumerFactory
                = new DefaultKafkaConsumerFactory<>(props);

        ConcurrentKafkaListenerContainerFactory<String, TradeEventProto> factory
                = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        return factory;
    }

    @Bean(name = "jsonKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, TradeDto> jsonKafkaListenerContainerFactory() {

        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "validation-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonDeserializer.class);
        props.put("spring.json.trusted.packages", "*");

        DefaultKafkaConsumerFactory<String, TradeDto> consumerFactory
                = new DefaultKafkaConsumerFactory<>(
                        props,
                        new StringDeserializer(),
                        new org.springframework.kafka.support.serializer.JsonDeserializer<>(TradeDto.class)
                );

        ConcurrentKafkaListenerContainerFactory<String, TradeDto> factory
                = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);

        return factory;
    }

}
