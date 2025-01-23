using Confluent.Kafka;
using System;

class KafkaTransactions {
    static void Main(string[] args) {
        var producerConfig = new ProducerConfig {
            BootstrapServers = "localhost:9092",
            TransactionalId = "txn-csharp",
            EnableIdempotence = true
        };

        var consumerConfig = new ConsumerConfig {
            BootstrapServers = "localhost:9092",
            GroupId = "exactly-once-group",
            IsolationLevel = IsolationLevel.ReadCommitted
        };

        using var producer = new ProducerBuilder<Null, string>(producerConfig).Build();
        using var consumer = new ConsumerBuilder<Null, string>(consumerConfig).Build();

        producer.InitTransactions();
        consumer.Subscribe("input-topic");

        while (true) {
            var consumeResult = consumer.Consume();
            producer.BeginTransaction();
            try {
                var transformedValue = consumeResult.Message.Value.ToUpper(); // Example transformation
                producer.Produce("output-topic", new Message<Null, string> { Value = transformedValue });
                producer.CommitTransaction();
            } catch (Exception e) {
                producer.AbortTransaction();
                Console.WriteLine($"Transaction aborted: {e.Message}");
            }
        }
    }
}