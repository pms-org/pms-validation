package com.pms.validation.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import com.pms.validation.proto.TradeEventProto;
import com.pms.validation.proto.InvalidTradeEventProto;
import com.google.protobuf.MessageLite;

import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;

@Configuration
public class KafkaConfig {

	@Value("${app.incoming-trades-topic}")
	private String incomingTradesTopic;

	@Value("${app.outgoing-valid-trades-topic}")
	private String outgoingValidTradesTopic;

	@Value("${app.outgoing-invalid-trades-topic}")
	private String outgoingInvalidTradesTopic;

	@Value("${spring.kafka.consumer.group-id}")
	private String consumerGroupId;

	@Value("${spring.kafka.bootstrap-servers}")
	private String kafkaBootstrapServers;

	@Value("${schema.registry.url}")
	private String schemaRegistryUrl;

	@Bean
	NewTopic validationTopic() {
		return TopicBuilder.name(outgoingValidTradesTopic)
				.partitions(5)
				.replicas(1)
				.build();
	}

	@Bean
	NewTopic incomingTopic() {
		return TopicBuilder.name(incomingTradesTopic)
				.partitions(5)
				.replicas(1)
				.build();
	}

	@Bean
	NewTopic invalidTradeTopic() {
		return TopicBuilder.name(outgoingInvalidTradesTopic)
				.partitions(5)
				.replicas(1)
				.build();
	}

	@Bean
	ProducerFactory<String, TradeEventProto> producerFactory() {

		Map<String, Object> props = new HashMap<>();

		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		// Protobuf serializer
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class);

		props.put("schema.registry.url", schemaRegistryUrl);

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
	KafkaTemplate<String, TradeEventProto> kafkaTemplate() {
		return new KafkaTemplate<>(producerFactory());
	}

	@Bean
	ProducerFactory<String, InvalidTradeEventProto> invalidTradeProducerFactory() {

		Map<String, Object> props = new HashMap<>();

		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		// Protobuf serializer
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class);

		props.put("schema.registry.url", schemaRegistryUrl);

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
	KafkaTemplate<String, InvalidTradeEventProto> invalidTradeKafkaTemplate() {
		return new KafkaTemplate<>(invalidTradeProducerFactory());
	}

	// Generic KafkaTemplate for RTTM Client to send any MessageLite (protobuf
	// messages)
	@Bean
	ProducerFactory<String, MessageLite> messageLiteProducerFactory() {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
				io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class);
		props.put("schema.registry.url", schemaRegistryUrl);

		// Retry configuration
		props.put(ProducerConfig.RETRIES_CONFIG, 5);
		props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
		props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);
		props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 15000);

		// Safe producer
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

		return new DefaultKafkaProducerFactory<>(props);
	}

	@Bean
	KafkaTemplate<String, MessageLite> messageLiteKafkaTemplate() {
		return new KafkaTemplate<>(messageLiteProducerFactory());
	}

	// Bean for metrics consumer - used by QueueMetricsService to query offsets
	@Bean
	KafkaConsumer<String, String> metricsConsumer() {
		Properties props = new Properties();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		return new KafkaConsumer<>(props);
	}

	@Bean(name = "protobufKafkaListenerContainerFactory")
	public ConcurrentKafkaListenerContainerFactory<String, TradeEventProto> protobufKafkaListenerContainerFactory() {

		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
		props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);

		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		// IMPORTANT: Protobuf deserializer
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				KafkaProtobufDeserializer.class);

		// REQUIRED
		props.put("schema.registry.url", schemaRegistryUrl);

		// IMPORTANT: Tell Kafka which Protobuf type to convert the bytes into
		props.put("specific.protobuf.value.type",
				"com.pms.validation.proto.TradeEventProto");

		DefaultKafkaConsumerFactory<String, TradeEventProto> consumerFactory = new DefaultKafkaConsumerFactory<>(
				props);

		ConcurrentKafkaListenerContainerFactory<String, TradeEventProto> factory = new ConcurrentKafkaListenerContainerFactory<>();

		factory.setConsumerFactory(consumerFactory);

		// Enable batch listener so KafkaListener can receive List<Message>
		factory.setBatchListener(true);

		// Use manual ack mode so listener can acknowledge after successful processing
		factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

		return factory;
	}

}
