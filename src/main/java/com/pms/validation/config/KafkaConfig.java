package com.pms.validation.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.pms.validation.entity.ValidationOutboxEntity;

@Configuration
public class KafkaConfig {

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
        public ConcurrentKafkaListenerContainerFactory<String, ValidationOutboxEntity> kafkaListenerContainerFactory() {

                Map<String, Object> props = new HashMap<>();
                props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
                props.put(ConsumerConfig.GROUP_ID_CONFIG, "validation-consumer-group");
                props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

                JsonDeserializer<ValidationOutboxEntity> deserializer = new JsonDeserializer<>(
                                ValidationOutboxEntity.class);
                deserializer.addTrustedPackages("*");

                DefaultKafkaConsumerFactory<String, ValidationOutboxEntity> consumerFactory = new DefaultKafkaConsumerFactory<>(
                                props,
                                new StringDeserializer(),
                                deserializer);

                ConcurrentKafkaListenerContainerFactory<String, ValidationOutboxEntity> factory = new ConcurrentKafkaListenerContainerFactory<>();
                factory.setConsumerFactory(consumerFactory);
                return factory;
        }
}
