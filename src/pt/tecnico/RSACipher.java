package pt.tecnico;

import java.io.FileInputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;

public class RSACipher {
    private static int DEC_BYTES = 117;
    private static int ENC_BYTES = 128;

    public byte[] encrypt(byte[] byteArray, String keyPath, int keyType) {
        try {
            System.out.println("-");
            

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            Key key = getKey(keyPath, keyType);

            System.out.println("Encrypting...");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            int size = (int) (byteArray.length / ((float) this.DEC_BYTES) * this.ENC_BYTES);
            byte[] eByteArray = new byte[size];
            System.out.println(size);
            for (int iByte = 0; iByte < (int) (byteArray.length / (float) this.DEC_BYTES); iByte++) {
                byte[] cBytes = cipher.doFinal(
                                        byteArray, 
                                        iByte * this.DEC_BYTES, 
                                        (this.DEC_BYTES * (iByte + 1) > byteArray.length) ? byteArray.length - iByte * this.DEC_BYTES : this.DEC_BYTES
                                    );
                System.arraycopy(cBytes, 0, eByteArray, iByte * this.ENC_BYTES, cBytes.length);
            }
            System.out.println("-");
            return eByteArray;
        } catch (Exception e) {
            // Pokemon exception handling!
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decrypt(byte[] eByteArray, String keyPath, int keyType) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            Key key = getKey(keyPath, keyType);

            System.out.println("Deciphering...");
            cipher.init(Cipher.DECRYPT_MODE, key);

            int size = (int) (eByteArray.length / ((float) this.DEC_BYTES) * this.ENC_BYTES);
            byte[] byteArray = new byte[size];
            for (int iByte = 0; iByte < (int) (eByteArray.length / (float) this.ENC_BYTES); iByte++) {
                byte[] dBytes = cipher.doFinal(
                                        eByteArray, 
                                        iByte * this.ENC_BYTES, 
                                        (this.ENC_BYTES * (iByte + 1) > eByteArray.length) ? eByteArray.length - iByte * this.ENC_BYTES : this.ENC_BYTES
                                    );
                System.arraycopy(dBytes, 0, byteArray, iByte * this.DEC_BYTES, dBytes.length);
            }
            return byteArray;
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