from confluent_kafka import Producer, Consumer, KafkaError

producer_conf = {
    'bootstrap.servers': 'localhost:9092',
    'transactional.id': 'txn-python',
    'enable.idempotence': True
}
producer = Producer(producer_conf)
producer.init_transactions()

consumer_conf = {
    'bootstrap.servers': 'localhost:9092',
    'group.id': 'exactly-once-python-group',
    'auto.offset.reset': 'earliest',
    'isolation.level': 'read_committed'
}
consumer = Consumer(consumer_conf)
consumer.subscribe(['input-topic'])

try:
    while True:
        msg = consumer.poll(1.0)
        if msg is None:
            continue

        if msg.error():
            if msg.error().code() == KafkaError._PARTITION_EOF:
                continue
            else:
                print(f"Error: {msg.error()}")
                break

        producer.begin_transaction()
        try:
            transformed_value = msg.value().decode('utf-8').upper()  # Example transformation
            producer.produce('output-topic', key=msg.key(), value=transformed_value)
            producer.commit_transaction()
        except Exception as e:
            producer.abort_transaction()
            print(f"Transaction aborted: {e}")
finally:
    consumer.close()