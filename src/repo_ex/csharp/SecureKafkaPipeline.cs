using System;
using System.IO;
using System.Security.Cryptography;
using System.Text;
using Confluent.Kafka;

class SecureKafkaPipeline {
    public static string Encrypt(string plainText, byte[] key, byte[] iv) {
        using (Aes aes = Aes.Create()) {
            aes.Key = key;
            aes.IV = iv;
            using (var encryptor = aes.CreateEncryptor(aes.Key, aes.IV)) {
                using (var ms = new MemoryStream()) {
                    using (var cs = new CryptoStream(ms, encryptor, CryptoStreamMode.Write)) {
                        using (var writer = new StreamWriter(cs)) {
                            writer.Write(plainText);
                        }
                    }
                    return Convert.ToBase64String(ms.ToArray());
                }
            }
        }
    }

    public static string Decrypt(string cipherText, byte[] key, byte[] iv) {
        using (Aes aes = Aes.Create()) {
            aes.Key = key;
            aes.IV = iv;
            using (var decryptor = aes.CreateDecryptor(aes.Key, aes.IV)) {
                using (var ms = new MemoryStream(Convert.FromBase64String(cipherText))) {
                    using (var cs = new CryptoStream(ms, decryptor, CryptoStreamMode.Read)) {
                        using (var reader = new StreamReader(cs)) {
                            return reader.ReadToEnd();
                        }
                    }
                }
            }
        }
    }

    public static void Main(string[] args) {
        // RSA keys for digital signatures
        using (RSACryptoServiceProvider rsa = new RSACryptoServiceProvider(2048)) {
            // AES keys for encryption
            byte[] key = Aes.Create().Key;
            byte[] iv = Aes.Create().IV;

            // Kafka producer and consumer configurations
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

                try {
                    string originalData = consumeResult.Message.Value;

                    // Digital signature
                    byte[] originalDataBytes = Encoding.UTF8.GetBytes(originalData);
                    byte[] signature = rsa.SignData(originalDataBytes, new SHA256CryptoServiceProvider());
                    bool isVerified = rsa.VerifyData(originalDataBytes, new SHA256CryptoServiceProvider(), signature);

                    if (!isVerified) {
                        Console.WriteLine("Data verification failed. Skipping message...");
                        continue;
                    }

                    // Encrypt data
                    string encryptedData = Encrypt(originalData, key, iv);
                    Console.WriteLine($"Encrypted Data: {encryptedData}");

                    // Kafka transaction
                    producer.BeginTransaction();
                    producer.Produce("output-topic", new Message<Null, string> { Value = encryptedData });
                    producer.CommitTransaction();

                    // Decrypt data (for verification)
                    string decryptedData = Decrypt(encryptedData, key, iv);
                    Console.WriteLine($"Decrypted Data: {decryptedData}");
                } catch (Exception e) {
                    producer.AbortTransaction();
                    Console.WriteLine($"Transaction aborted: {e.Message}");
                }
            }
        }
    }
}
