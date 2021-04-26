package com.vzcatus13.encryptednotes.utils;

import android.os.Build;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Encryption {

    private final static int IV_LENGTH = 12;
    private final static int SALT_LENGTH = 32;
    private final static int ITERATION_COUNT = 32768;
    private final static int AES_KEY_LENGTH = 256;
    private final static int TAG_LENGTH = 128;

    /**
     * Encrypt inputted data
     * @param data Byte array to be encrypted
     * @param password Char array with password
     * @return Encrypted byte array
     */
    public static byte[] encrypt(byte[] data, char[] password) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        byte[] iv = Utils.getRandomBytes(IV_LENGTH);
        byte[] salt = Utils.getRandomBytes(SALT_LENGTH);
        SecretKey secretKey = getAesKeyFromPassword(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));
        byte[] encryptedData = cipher.doFinal(data);

        return ByteBuffer.allocate(iv.length + salt.length + encryptedData.length)
                .put(iv)
                .put(salt)
                .put(encryptedData)
                .array();
    }

    /**
     * Decrypt inputted data
     * @param data Byte array to be decrypted
     * @param password Char array with password
     * @return Plain byte array
     */
    public static byte[] decrypt(byte[] data, char[] password) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, InvalidKeySpecException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byte[] iv = new byte[IV_LENGTH];
        byteBuffer.get(iv);
        byte[] salt = new byte[SALT_LENGTH];
        byteBuffer.get(salt);
        byte[] encryptedData = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedData);
        SecretKey secretKey = getAesKeyFromPassword(password, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH, iv));

        return cipher.doFinal(encryptedData);
    }

    /**
     * Get AES key from given password and salt
     * @param password Char array with password
     * @param salt Byte array with salt
     * @return SecretKey instance
     */
    private static SecretKey getAesKeyFromPassword(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory secretKeyFactory;
        KeySpec keySpec;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA256"); // PBKDF2withHmacSHA256 API 26+
        } else {
            secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2withHmacSHA1"); // PBKDF2withHmacSHA1 API 10+

        }
        keySpec = new PBEKeySpec(password, salt, ITERATION_COUNT, AES_KEY_LENGTH);
        return new SecretKeySpec(secretKeyFactory.generateSecret(keySpec).getEncoded(), "AES");
    }
}
