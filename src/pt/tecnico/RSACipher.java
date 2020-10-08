package pt.tecnico;

import java.io.*;
import java.io.FileInputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

import javax.crypto.SealedObject;

public class RSACipher {
    private static int DEC_BYTES = 117;
    private static int ENC_BYTES = 128;

    public SealedObject encrypt(Serializable object, String keyPath, int keyType) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            Key key = getKey(keyPath, keyType);

            System.out.println("Encrypting...");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            SealedObject eObject = new SealedObject(object, cipher);

            return eObject;
        } catch (Exception e) {
            // Pokemon exception handling!
            e.printStackTrace();
        }
        return null;
    }

    public Object decrypt(SealedObject eObject, String keyPath, int keyType) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            Key key = getKey(keyPath, keyType);

            System.out.println("Deciphering...");
            cipher.init(Cipher.DECRYPT_MODE, key);
            Object object = (String) eObject.getObject(cipher);
            return object;
        } catch (Exception e) {
            // Pokemon exception handling!
            e.printStackTrace();
        }
        return null;
    }

    private Key getKey(String keyPath, int keyType) {
        // Read key
        try {
            FileInputStream fis = new FileInputStream(keyPath);
            byte[] keyEncoded = new byte[fis.available()];
            fis.read(keyEncoded);
            fis.close();

            KeyFactory keyFac = KeyFactory.getInstance("RSA");
            Key key = null;
            if (keyType == Cipher.PUBLIC_KEY) {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyEncoded);
                key = keyFac.generatePublic(keySpec);
            } else if (keyType == Cipher.PRIVATE_KEY) {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyEncoded);
                key = keyFac.generatePrivate(keySpec);
            }
            return key;
        } catch (Exception e) {
            // Pokemon exception handling!
            e.printStackTrace();
        }
        return null;
    }
}