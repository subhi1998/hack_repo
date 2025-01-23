#include <iostream>
#include <librdkafka/rdkafkacpp.h>

int main() {
    std::string brokers = "localhost:9092";
    std::string inputTopic = "input-topic";
    std::string outputTopic = "output-topic";

    RdKafka::Conf *conf = RdKafka::Conf::create(RdKafka::Conf::CONF_GLOBAL);
    conf->set("bootstrap.servers", brokers, errstr);

    RdKafka::Producer *producer = RdKafka::Producer::create(conf, errstr);
    if (!producer) {
        std::cerr << "Failed to create producer: " << errstr << std::endl;
        return 1;
    }

    producer->init_transactions(5000); // Init transactional producer
    producer->begin_transaction();

    try {
        for (int i = 0; i < 10; i++) {
            std::string value = "Message " + std::to_string(i);
            producer->produce(outputTopic, RdKafka::Topic::PARTITION_UA, RdKafka::Producer::RK_MSG_COPY,
                              const_cast<char *>(value.c_str()), value.size(), nullptr, nullptr);
        }
        producer->commit_transaction(5000);
    } catch (std::exception &e) {
        producer->abort_transaction();
        std::cerr << "Transaction aborted: " << e.what() << std::endl;
    }

    delete producer;
    return 0;
}