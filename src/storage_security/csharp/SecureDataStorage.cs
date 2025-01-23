using System;
using System.IO;
using System.Security.Cryptography;
using System.Text;

class SecureDataStorage {
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

    public static void Main() {
        string sensitiveData = "SensitiveData123";
        byte[] key = Aes.Create().Key;
        byte[] iv = Aes.Create().IV;

        string encryptedData = Encrypt(sensitiveData, key, iv);
        Console.WriteLine("Encrypted Data: " + encryptedData);

        string decryptedData = Decrypt(encryptedData, key, iv);
        Console.WriteLine("Decrypted Data: " + decryptedData);
    }
}