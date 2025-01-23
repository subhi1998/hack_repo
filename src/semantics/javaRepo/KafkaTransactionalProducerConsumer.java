package semantics.javaRepo;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaTransactionalProducerConsumer {
    public static void main(String[] args) {
        String inputTopic = "input-topic";
        String outputTopic = "output-topic";
        String bootstrapServers = "localhost:9092";

        // Consumer properties
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "exactly-once-group");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        // Producer properties
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "transaction-id-1");
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        producer.initTransactions();
        consumer.subscribe(Collections.singletonList(inputTopic));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                if (!records.isEmpty()) {
                    producer.beginTransaction();
                    try {
                        for (ConsumerRecord<String, String> record : records) {
                            String transformedValue = record.value().toUpperCase(); // Example transformation
                            producer.send(new ProducerRecord<>(outputTopic, record.key(), transformedValue));
                        }
                        producer.sendOffsetsToTransaction(
                                consumer.assignment().stream()
                                        .collect(Collectors.toMap(
                                                tp -> tp,
                                                tp -> new OffsetAndMetadata(consumer.position(tp)))),
                                consumer.groupMetadata());
                        producer.commitTransaction();
                    } catch (Exception e) {
                        producer.abortTransaction();
                        e.printStackTrace();
                    }
                }
            }
        } finally {
            consumer.close();
        }
    }
}
