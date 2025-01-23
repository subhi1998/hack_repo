#include <iostream>
#include <cryptopp/rsa.h>
#include <cryptopp/osrng.h>
#include <cryptopp/aes.h>
#include <cryptopp/modes.h>
#include <cryptopp/filters.h>
#include <librdkafka/rdkafkacpp.h>

using namespace CryptoPP;

// RSA Key Generation and Exchange
void generateRSAKeys() {
    AutoSeededRandomPool rng;

    RSA::PrivateKey privateKey;
    privateKey.GenerateRandomWithKeySize(rng, 2048);
    RSA::PublicKey publicKey(privateKey);

    // ** In a real-world scenario, you would securely exchange the public key
    // ** with the intended recipient.

    // Example:
    // std::cout << "Public Key: " << publicKey << std::endl;
}

// Encrypt AES Key with RSA
std::string encryptAESKeyWithRSA(const RSA::PublicKey& publicKey, const std::string& aesKey) {
    std::string cipher;
    RSAES_OAEP_SHA_Encryptor e(publicKey);
    StringSource ss1(aesKey, true,
        new PK_EncryptorFilter(rng, e,
            new StringSink(cipher)
        ) // PK_EncryptorFilter
    ); // StringSource
    return cipher;
}

// Decrypt AES Key with RSA
std::string decryptAESKeyWithRSA(const RSA::PrivateKey& privateKey, const std::string& encryptedAESKey) {
    std::string recovered;
    RSAES_OAEP_SHA_Decryptor d(privateKey);
    StringSource ss2(encryptedAESKey, true,
        new PK_DecryptorFilter(rng, d,
            new StringSink(recovered)
        ) // PK_DecryptorFilter
    ); // StringSource
    return recovered;
}

// AES Encryption
std::string encryptAES(const std::string& key, const std::string& message) {
    AES::Encryption aesEncryption((byte *)key.data(), AES::DEFAULT_KEYLENGTH);
    ECB_Mode_ExternalCipher::Encryption ecbEncryption(aesEncryption);

    std::string encrypted;
    StringSource ss3(message, true,
        new StreamTransformationFilter(ecbEncryption,
            new StringSink(encrypted)
        ) // StreamTransformationFilter
    ); // StringSource
    return encrypted;
}

// AES Decryption
std::string decryptAES(const std::string& key, const std::string& encryptedMessage) {
    AES::Decryption aesDecryption((byte *)key.data(), AES::DEFAULT_KEYLENGTH);
    ECB_Mode_ExternalCipher::Decryption ecbDecryption(aesDecryption);

    std::string decrypted;
    StringSource ss4(encryptedMessage, true,
        new StreamTransformationFilter(ecbDecryption,
            new StringSink(decrypted)
        ) // StreamTransformationFilter
    ); // StringSource
    return decrypted;
}

// Kafka Producer (Transactional)
void produceToKafka(const std::string& brokers, const std::string& topic, const std::string& message) {
    std::string errstr;
    RdKafka::Conf *conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);
    conf->set("bootstrap.servers", brokers, errstr);

    RdKafka::Producer *producer = RdKafka::Producer::create(conf, errstr);
    if (!producer) {
        std::cerr << "Failed to create producer: " << errstr << std::endl;
        return;
    }

    producer->init_transactions(5000); // Init transactional producer
    producer->begin_transaction();

    try {
        producer->produce(topic, RdKafka::Topic::PARTITION_UA, RdKafka::Producer::RK_MSG_COPY,
                          const_cast<char *>(message.c_str()), message.size(), nullptr, nullptr);
        producer->commit_transaction(5000);
    } catch (std::exception &e) {
        producer->abort_transaction();
        std::cerr << "Transaction aborted: " << e.what() << std::endl;
    }

    delete producer;
}

int main() {
    // 1. Generate RSA Keys
    generateRSAKeys();

    // 2. Generate AES Key
    std::string aesKey = "AESKEY1234567890"; // Example AES Key

    // 3. Encrypt AES Key with RSA (using the public key)
    // In a real-world scenario, you would obtain the public key from the recipient.
    // For this example, we assume you have the public key available.
    std::string encryptedAESKey = encryptAESKeyWithRSA(publicKey, aesKey);

    // 4. Encrypt Message with AES
    std::string message = "Hello, Hybrid Encryption!";
    std::string encryptedMessage = encryptAES(aesKey, message);

    // 5. (Optional) Produce Encrypted Message to Kafka
    std::string brokers = "localhost:9092";
    std::string outputTopic = "output-topic";
    produceToKafka(brokers, outputTopic, encryptedMessage);

    // 6. (Optional) Decrypt Message (for demonstration)
    // In a real-world scenario, the recipient would decrypt the AES key
    // using their private key and then decrypt the message.
    std::string decryptedAESKey = decryptAESKeyWithRSA(privateKey, encryptedAESKey);
    std::string decryptedMessage = decryptAES(decryptedAESKey, encryptedMessage);
    std::cout << "Decrypted Message: " << decryptedMessage << std::endl;

    return 0;
}