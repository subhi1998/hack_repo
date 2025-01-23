from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives import padding
from cryptography.hazmat.backends import default_backend
import os

def encrypt(data, key):
    cipher = Cipher(algorithms.AES(key), modes.CBC(os.urandom(16)), backend=default_backend())
    encryptor = cipher.encryptor()
    padder = padding.PKCS7(algorithms.AES.block_size).padder()
    padded_data = padder.update(data.encode()) + padder.finalize()
    return cipher.encryptor().update(padded_data) + cipher.encryptor().finalize()

def decrypt(encrypted_data, key):
    cipher = Cipher(algorithms.AES(key), modes.CBC(encrypted_data[:16]), backend=default_backend())
    decryptor = cipher.decryptor()
    unpadder = padding.PKCS7(algorithms.AES.block_size).unpadder()
    decrypted_data = unpadder.update(decryptor.update(encrypted_data[16:]) + decryptor.finalize())
    return decrypted_data.decode()

if __name__ == "__main__":
    key = os.urandom(16)
    sensitive_data = "SecurePassword123"

    encrypted_data = encrypt(sensitive_data, key)
    print(f"Encrypted Data: {encrypted_data}")

    decrypted_data = decrypt(encrypted_data, key)
    print(f"Decrypted Data: {decrypted_data}")