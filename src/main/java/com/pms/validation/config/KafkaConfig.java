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
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.pms.validation.dto.TradeDto;
import com.pms.validation.proto.TradeEventProto;

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

		// Protobuf serializer
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class);

		props.put("schema.registry.url", "http://localhost:8081");

		// Retry 5 times
		props.put(ProducerConfig.RETRIES_CONFIG, 5);

		// Delay between retries (500ms default)
		props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 1 sec

		// Maximum time allowed for send including retries
		props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000); // 30 sec total timeout

		// Timeout waiting for broker ack
		props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 15000); // 15 sec

		// Ensure safe producer (no duplicates)
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

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
				KafkaProtobufDeserializer.class);

		// REQUIRED
		props.put("schema.registry.url", "http://localhost:8081");

		// IMPORTANT: Tell Kafka which Protobuf type to convert the bytes into
		props.put("specific.protobuf.value.type",
				"com.pms.validation.proto.TradeEventProto");

		DefaultKafkaConsumerFactory<String, TradeEventProto> consumerFactory = new DefaultKafkaConsumerFactory<>(
				props);

		ConcurrentKafkaListenerContainerFactory<String, TradeEventProto> factory = new ConcurrentKafkaListenerContainerFactory<>();

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
				JsonDeserializer.class);
		props.put("spring.json.trusted.packages", "*");

		DefaultKafkaConsumerFactory<String, TradeDto> consumerFactory = new DefaultKafkaConsumerFactory<>(
				props,
				new StringDeserializer(),
				new JsonDeserializer<>(TradeDto.class));

		ConcurrentKafkaListenerContainerFactory<String, TradeDto> factory = new ConcurrentKafkaListenerContainerFactory<>();

		factory.setConsumerFactory(consumerFactory);

		return factory;
	}

}
