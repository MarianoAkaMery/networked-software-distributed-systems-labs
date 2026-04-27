package it.polimi.nsds.kafka.eval;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;

import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.Duration;
import java.util.*;

// Group number: 36
// Group members: Rong Huang | Salvatore Mariano Librici | Mohammadali Amiri

//1) Is it possible to have more than one partition for topics "sensors1" and "sensors2"?

// Yes. Both topics can have multiple partitions to increase parallelism. However, to
// correctly implement the Merger (which needs to see, for a given key, the values from
// both sensors1 and sensors2 on the same consumer instance), we assume that:
//  - both sensors1 and sensors2 use key-based partitioning
//  - they have the same number of partitions
// In this way, the same key is always mapped to the same partition index in both topics,
// and Kafka assigns partitions consistently to consumers in the same group.

//2) Is there any relation between the number of partitions in "sensors1" and "sensors2"?

// Yes. To keep the Merger simple and local (no cross-partition communication), the
// number of partitions of sensors1 and sensors2 should be the same. With equal
// partition counts and deterministic partitioning, partition i of sensors1 and
// partition i of sensors2 will be assigned to the same consumer instance in a group.

//3) Is it possible to have more than one instance of Merger?

// Yes. We can run multiple Merger instances for scalability and fault tolerance.

//4) If so, what is the relation between their group id?

// All Merger instances that cooperate to process the same logical stream must use
// the SAME consumer group id. Then Kafka will partition the set of topic partitions
// among them, ensuring that each partition (and thus each key) is processed by
// exactly one Merger instance. Different group ids would cause each instance to
// read the full stream and duplicate the work and outputs.

//5) Is it possible to have more than one partition for topic "merged"?

// Yes. The merged topic can also have multiple partitions (e.g., same number as
// sensors1/sensors2). When the Merger sends records using the same keys, Kafka will
// partition them consistently, preserving per-key ordering and allowing downstream
// consumers (like Validator) to scale as well.

//6) Is it possible to have more than one instance of Validator?

// Yes. We can also run multiple Validator instances.

//7) If so, what is the relation between their group id?

// All Validator instances that share the work on the merged topic should use the
// SAME consumer group id, so Kafka splits the merged partitions among them and each
// message is processed by exactly one Validator. Different group ids would make each
// Validator see all messages and produce duplicate outputs.

public class Consumers36 {

    private static final String SENSORS1_TOPIC = "sensors1";
    private static final String SENSORS2_TOPIC = "sensors2";
    private static final String MERGED_TOPIC = "merged";
    private static final String OUTPUT1_TOPIC = "output1";
    private static final String OUTPUT2_TOPIC = "output2";

    public static void main(String[] args) {
        String serverAddr = "localhost:9092";
        int stage = Integer.parseInt(args[0]);
        String groupId = args[1];
        switch (stage) {
            case 0:
                new Merger(serverAddr, groupId).execute();
                break;
            case 1:
                new Validator(serverAddr, groupId).execute();
                break;
            case 2:
                System.err.println("Wrong stage");
        }

    }

    // ============================
    // MERGER
    // ============================
    private static class Merger {
        private final String serverAddr;
        private final String consumerGroupId;

        // State: last value seen from each topic per key
        private final Map<String, Integer> lastFromSensors1 = new HashMap<>();
        private final Map<String, Integer> lastFromSensors2 = new HashMap<>();

        public Merger(String serverAddr, String consumerGroupId) {
            this.serverAddr = serverAddr;
            this.consumerGroupId = consumerGroupId;
        }

        public void execute() {
            // Consumer
            final Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
            consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // manual commit
            consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());

            KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Arrays.asList(SENSORS1_TOPIC, SENSORS2_TOPIC));

            // Producer
            final Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
            // producerProps.put(ProducerConfig.ACKS_CONFIG, "all");

            final KafkaProducer<String, Integer> producer = new KafkaProducer<>(producerProps);

            System.out.println("Merger started. Group: " + consumerGroupId);

            while (true) {
                ConsumerRecords<String, Integer> records = consumer.poll(Duration.ofMillis(1000));

                if (records.isEmpty()) {
                    continue;
                }

                for (ConsumerRecord<String, Integer> record : records) {
                    String topic = record.topic();
                    String key = record.key();
                    Integer value = record.value();

                    // Update local state
                    if (SENSORS1_TOPIC.equals(topic)) {
                        lastFromSensors1.put(key, value);
                    } else if (SENSORS2_TOPIC.equals(topic)) {
                        lastFromSensors2.put(key, value);
                    }

                    // Now LET'S cOMPUTE THE sum
                    int v1 = lastFromSensors1.getOrDefault(key, 0);
                    int v2 = lastFromSensors2.getOrDefault(key, 0);
                    int sum = v1 + v2;

                    ProducerRecord<String, Integer> outRecord = new ProducerRecord<>(MERGED_TOPIC, key, sum);
                    producer.send(outRecord);

                    System.out.println(
                            "[MERGER] inTopic=" + topic +
                                    " partition=" + record.partition() +
                                    " key=" + key +
                                    " | " +
                                    " value=" + value +
                                    " --> mergedSum=" + sum);
                }

                // Ensure all produced messages are stored before committing
                producer.flush();

                // At-least-once semantics:
                // - If we crash AFTER flush but BEFORE commit, these records
                // will be reprocessed and cause duplicated outputs (minimal).
                // - If we crash BEFORE flush, offsets are not advanced, so
                // we will reprocess and not lose messages.
                try {
                    consumer.commitSync();
                } catch (CommitFailedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    // ============================
    // VALIDATOR
    // ============================
    private static class Validator {
        private final String serverAddr;
        private final String consumerGroupId;

        public Validator(String serverAddr, String consumerGroupId) {
            this.serverAddr = serverAddr;
            this.consumerGroupId = consumerGroupId;
        }

        public void execute() {
            // Consumer
            final Properties consumerProps = new Properties();
            consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, String.valueOf(consumerGroupId));
            consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // manual offset management
            consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
            // Added PROPERTIES
            consumerProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

            KafkaConsumer<String, Integer> consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Collections.singletonList(MERGED_TOPIC));

            final Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, serverAddr);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());

            producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
            producerProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,
                    "validator-" + consumerGroupId + "-" + System.currentTimeMillis());

            KafkaProducer<String, Integer> producer = new KafkaProducer<>(producerProps);

            producer.initTransactions();

            System.out.println("Validator started. Group: " + consumerGroupId);

            while (true) {
                ConsumerRecords<String, Integer> records = consumer.poll(Duration.ofMillis(1000));

                if (records.isEmpty()) {
                    continue;
                }

                // Start transactionS
                producer.beginTransaction();

                Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();

                for (ConsumerRecord<String, Integer> record : records) {
                    String key = record.key();
                    Integer value = record.value();
                    if (key == null || value == null) {
                        continue;
                    }

                    // Send to both output
                    ProducerRecord<String, Integer> out1 = new ProducerRecord<>(OUTPUT1_TOPIC, key, value);
                    ProducerRecord<String, Integer> out2 = new ProducerRecord<>(OUTPUT2_TOPIC, key, value);

                    producer.send(out1);
                    producer.send(out2);

                    System.out.println(
                            "[VALIDATOR] merged partition=" + record.partition() +
                                    " key=" + key +
                                    " | " +
                                    " value=" + value +
                                    " --> forwarded to " + OUTPUT1_TOPIC +
                                    " and " + OUTPUT2_TOPIC);

                    TopicPartition tp = new TopicPartition(record.topic(), record.partition());
                    // offset to commit = current offset + 1
                    offsetsToCommit.put(tp, new OffsetAndMetadata(record.offset() + 1));
                }

                producer.sendOffsetsToTransaction(offsetsToCommit, consumer.groupMetadata());
                producer.commitTransaction();

            }

        }
    }
}
