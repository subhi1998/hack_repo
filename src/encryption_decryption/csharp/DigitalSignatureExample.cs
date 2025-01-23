using System;
using System.Security.Cryptography;
using System.Text;

public class DigitalSignatureExample {
    public static void Main() {
        using (RSACryptoServiceProvider rsa = new RSACryptoServiceProvider(2048)) {
            string originalData = "Hello, Digital Signatures!";
            byte[] originalDataBytes = Encoding.UTF8.GetBytes(originalData);

            // Sign data
            byte[] signature = rsa.SignData(originalDataBytes, new SHA256CryptoServiceProvider());

            // Verify signature
            bool isVerified = rsa.VerifyData(originalDataBytes, new SHA256CryptoServiceProvider(), signature);

            Console.WriteLine($"Data verified: {isVerified}");
        }
    }
}