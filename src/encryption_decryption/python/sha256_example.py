import hashlib

def hash_data(data):
    sha256_hash = hashlib.sha256()
    sha256_hash.update(data.encode())
    return sha256_hash.hexdigest()

if __name__ == "__main__":
    data = "Hello, SHA-256!"
    hashed_data = hash_data(data)
    print(f"SHA-256 Hash: {hashed_data}")
