package encryption_decryption.javaRepo;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.*;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

public class CombinedSecurityKafka {

    // AES Example
    public static void aesExample() throws Exception {
        // Generate a Key for AES
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128); // 128-bit AES
        SecretKey secretKey = keyGenerator.generateKey();

        // Encrypt
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        String plainText = "Hello, World!";
        byte[] encryptedText = cipher.doFinal(plainText.getBytes());
        System.out.println("AES Encrypted: " + Base64.getEncoder().encodeToString(encryptedText));

        // Decrypt
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedText = cipher.doFinal(encryptedText);
        System.out.println("AES Decrypted: " + new String(decryptedText));
    }

    // RSA Example
    public static void rsaExample() throws Exception {
        // Generate KeyPair for RSA
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        // Encrypt
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        String plainText = "Hello, RSA!";
        byte[] encryptedText = cipher.doFinal(plainText.getBytes());
        System.out.println("RSA Encrypted: " + Base64.getEncoder().encodeToString(encryptedText));

        // Decrypt
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedText = cipher.doFinal(encryptedText);
        System.out.println("RSA Decrypted: " + new String(decryptedText));
    }

    // Kafka Idempotent Producer Consumer Example
    public static void kafkaIdempotentExample() {
        String inputTopic = "input-topic";
        String outputTopic = "output-topic";
        String bootstrapServers = "localhost:9092";

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "idempotent-group");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        KafkaProducer<String, String> producer = new KafkaProducer<>(producerProps);

        consumer.subscribe(Collections.singletonList(inputTopic));
        Set<String> processedOffsets = new ConcurrentSkipListSet<>();

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    String offsetKey = record.topic() + "-" + record.partition() + "-" + record.offset();
                    if (!processedOffsets.contains(offsetKey)) {
                        String transformedValue = record.value().toUpperCase();
                        producer.send(new ProducerRecord<>(outputTopic, record.key(), transformedValue));
                        processedOffsets.add(offsetKey);
                        System.out.println("Processed offset: " + offsetKey);
                    }
                }
                consumer.commitSync();
            }
        } finally {
            consumer.close();
        }
    }

    // Kafka Transactional Producer Consumer Example
    public static void kafkaTransactionalExample() {
        String inputTopic = "input-topic";
        String outputTopic = "output-topic";
        String bootstrapServers = "localhost:9092";

        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "exactly-once-group");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

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
                            String transformedValue = record.value().toUpperCase();
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

    public static void main(String[] args) throws Exception {
        System.out.println("Running AES Example...");
        aesExample();

        System.out.println("\nRunning RSA Example...");
        rsaExample();

        System.out.println("\nRunning Kafka Idempotent Example...");
        kafkaIdempotentExample();

        System.out.println("\nRunning Kafka Transactional Example...");
        kafkaTransactionalExample();
    }
}
