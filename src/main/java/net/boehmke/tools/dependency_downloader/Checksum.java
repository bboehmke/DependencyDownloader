/*
 * Copyright (c) 2016 by Benjamin Böhmke
 *
 * DependencyDownloader is free software; you can redistribute it and/or modify it under
 * the terms of the MIT License. See the LICENSE file for more details.
 */

package net.boehmke.tools.dependency_downloader;


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {
    /**
     * Calculate and compare a MD5 checksum for the file
     * @param path Path to the file
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static void checkMd5(String path, String checksum)
            throws IOException, NoSuchAlgorithmException {
        compareChecksum("MD5", path, checksum);
    }

    /**
     * Calculate a MD5 checksum for the file
     * @param path Path to the file
     * @return Hash of the file content
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String createMd5(String path)
            throws IOException, NoSuchAlgorithmException {
        return calculateChecksum("MD5", path);
    }


    /**
     * Calculate and compare a SHA1 checksum for the file
     * @param path Path to the file
     * @param checksum Expected checksum
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static void checkSha1(String path, String checksum)
            throws IOException, NoSuchAlgorithmException {
        compareChecksum("SHA1", path, checksum);
    }

    /**
     * Calculate a SHA1 checksum for the file
     * @param path Path to the file
     * @return Hash of the file content
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static String createSha1(String path)
            throws IOException, NoSuchAlgorithmException {
        return calculateChecksum("SHA1", path);
    }

    /**
     * Calculate and compare a hash of a file
     * @param algorithm Hash algorithm
     * @param path Path to the file
     * @param checksum Expected checksum
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private static void compareChecksum(String algorithm, String path,
                                           String checksum)
            throws IOException, NoSuchAlgorithmException {

        System.out.println("  Check " + algorithm + " checksum...");

        // create checksum
        String newHash = calculateChecksum(algorithm, path);

        // compare hash
        if (newHash.equals(checksum)) {
            System.out.println("  Checksum OK!");
        } else {
            throw new IOException("Invalid Checksum!\n Expected: " + checksum + "\n Get: " + newHash);
        }
    }

    /**
     * Calculate a hash of a file
     * @param algorithm Hash algorithm
     * @param path Path to the file
     * @return Hash of the file content
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private static String calculateChecksum(String algorithm, String path)
            throws IOException, NoSuchAlgorithmException {
        // get input stream
        InputStream in =  new FileInputStream(path);

        // get hash creator
        MessageDigest digest = MessageDigest.getInstance(algorithm);

        // prepare buffer
        byte[] buffer = new byte[1024];

        // read data
        int size;
        while ((size = in.read(buffer)) != -1) {
            digest.update(buffer, 0, size);
        }

        // close file
        in.close();

        // calculate checksum
        return byteToString(digest.digest());
    }

    /**
     * Convert a byte array to a hex string
     * @param data Byte array to convert
     * @return Hex string of byte array
     */
    private static String byteToString(byte[] data) {
        String string = "";
        for (byte d: data) {
            string += String.format("%02x", d);
        }
        return string;
    }
}
