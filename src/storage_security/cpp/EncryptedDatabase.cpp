#include <iostream>
#include <cryptopp/aes.h>
#include <cryptopp/modes.h>
#include <cryptopp/filters.h>
#include <cryptopp/osrng.h>

using namespace CryptoPP;

int main() {
    AutoSeededRandomPool rng;

    // Generate AES Key and IV
    SecByteBlock key(AES::DEFAULT_KEYLENGTH);
    rng.GenerateBlock(key, key.size());
    SecByteBlock iv(AES::BLOCKSIZE);
    rng.GenerateBlock(iv, iv.size());

    std::string plainText = "SensitiveData123";
    std::string cipherText, recoveredText;

    // Encrypt
    try {
        CBC_Mode<AES>::Encryption encryption(key, key.size(), iv);
        StringSource(plainText, true, new StreamTransformationFilter(encryption, new StringSink(cipherText)));
        std::cout << "Encrypted Text: " << cipherText << std::endl;
    } catch (const Exception &e) {
        std::cerr << e.what() << std::endl;
        return 1;
    }

    // Decrypt
    try {
        CBC_Mode<AES>::Decryption decryption(key, key.size(), iv);
        StringSource(cipherText, true, new StreamTransformationFilter(decryption, new StringSink(recoveredText)));
        std::cout << "Decrypted Text: " << recoveredText << std::endl;
    } catch (const Exception &e) {
        std::cerr << e.what() << std::endl;
        return 1;
    }

    return 0;
}