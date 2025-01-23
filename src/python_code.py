from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.asymmetric import utils
from cryptography.hazmat.primitives.serialization import load_pem_private_key, load_pem_public_key
from cryptography.hazmat.backends import default_backend
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding as sym_padding
from confluent_kafka import Producer, Consumer, KafkaError
import hashlib
import os

# RSA Digital Signature Example
def rsa_digital_signature():
    # Generate a new RSA key pair
    private_key = rsa.generate_private_key(
        public_exponent=65537,
        key_size=2048,
        backend=default_backend()
    )
    public_key = private_key.public_key()

    # Sign a message
    message = b"Hello, digital signatures!"
    signature = private_key.sign(
        message,
        padding.PSS(
            mgf=padding.MGF1(hashes.SHA256()),
            salt_length=padding.PSS.MAX_LENGTH
        ),
        hashes.SHA256()
    )

    # Verify the signature
    public_key.verify(
        signature,
        message,
        padding.PSS(
            mgf=padding.MGF1(hashes.SHA256()),
            salt_length=padding.PSS.MAX_LENGTH
        ),
        hashes.SHA256()
    )
    print("RSA Signature verified!")

# SHA-256 Hashing Example
def hash_data(data):
    sha256_hash = hashlib.sha256()
    sha256_hash.update(data.encode())
    return sha256_hash.hexdigest()

# Kafka Transactional Producer-Consumer Example
def kafka_transactional_example():
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
                transformed_value = msg.value().decode('utf-8').upper()
                producer.produce('output-topic', key=msg.key(), value=transformed_value)
                producer.commit_transaction()
            except Exception as e:
                producer.abort_transaction()
                print(f"Transaction aborted: {e}")
    finally:
        consumer.close()

# AES Encryption/Decryption Example
def encrypt(data, key):
    iv = os.urandom(16)
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()
    padder = sym_padding.PKCS7(algorithms.AES.block_size).padder()
    padded_data = padder.update(data.encode()) + padder.finalize()
    return iv + encryptor.update(padded_data) + encryptor.finalize()

def decrypt(encrypted_data, key):
    iv = encrypted_data[:16]
    cipher = Cipher(algorithms.AES(key), modes.CBC(iv), backend=default_backend())
    decryptor = cipher.decryptor()
    unpadder = sym_padding.PKCS7(algorithms.AES.block_size).unpadder()
    decrypted_data = unpadder.update(decryptor.update(encrypted_data[16:]) + decryptor.finalize())
    return decrypted_data.decode()

if __name__ == "__main__":
    # RSA Digital Signature
    print("Running RSA Digital Signature Example...")
    rsa_digital_signature()

    # SHA-256 Hashing
    data = "Hello, SHA-256!"
    hashed_data = hash_data(data)
    print(f"SHA-256 Hash: {hashed_data}")

    # Kafka Transactional Example
    print("\nRunning Kafka Transactional Example...")
    kafka_transactional_example()

    # AES Encryption/Decryption
    print("\nRunning AES Encryption/Decryption Example...")
    key = os.urandom(16)
    sensitive_data = "SecurePassword123"
    encrypted_data = encrypt(sensitive_data, key)
    print(f"Encrypted Data: {encrypted_data}")
    decrypted_data = decrypt(encrypted_data, key)
    print(f"Decrypted Data: {decrypted_data}")