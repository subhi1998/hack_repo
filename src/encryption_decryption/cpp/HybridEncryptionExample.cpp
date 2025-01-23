#include <iostream>
#include <cryptopp/rsa.h>
#include <cryptopp/osrng.h>
#include <cryptopp/aes.h>
#include <cryptopp/modes.h>
#include <cryptopp/filters.h>

using namespace CryptoPP;

int main() {
    AutoSeededRandomPool rng;

    // Generate RSA keys
    RSA::PrivateKey privateKey;
    privateKey.GenerateRandomWithKeySize(rng, 2048);
    RSA::PublicKey publicKey(privateKey);

    // Encrypt a symmetric key using RSA
    std::string plain = "AESKEY1234567890"; // A simple 128-bit AES key
    std::string cipher;
    RSAES_OAEP_SHA_Encryptor e(publicKey);
    StringSource ss1(plain, true,
        new PK_EncryptorFilter(rng, e,
            new StringSink(cipher)
        ) // PK_EncryptorFilter
    ); // StringSource

    // Decrypt the AES key
    std::string recovered;
    RSAES_OAEP_SHA_Decryptor d(privateKey);
    StringSource ss2(cipher, true,
        new PK_DecryptorFilter(rng, d,
            new StringSink(recovered)
        ) // PK_DecryptorFilter
    ); // StringSource

    std::cout << "Recovered key: " << recovered << std::endl;

    // Now use AES to encrypt something using the recovered key
    AES::Encryption aesEncryption((byte *)recovered.data(), AES::DEFAULT_KEYLENGTH);
    ECB_Mode_ExternalCipher::Encryption ecbEncryption(aesEncryption);

    std::string message = "Hello, Hybrid Encryption!";
    std::string encrypted;
    StringSource ss3(message, true,
        new StreamTransformationFilter(ecbEncryption,
            new StringSink(encrypted)
        ) // StreamTransformationFilter
    ); // StringSource

    std::cout << "Encrypted message: " << encrypted << std::endl;
    return 0;
}
