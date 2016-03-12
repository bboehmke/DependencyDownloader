/*
 * Copyright (c) 2016 by Benjamin Böhmke
 *
 * DependencyDownloader is free software; you can redistribute it and/or modify it under
 * the terms of the MIT License. See the LICENSE file for more details.
 */

package net.boehmke.tools.dep_downloader;

import java.io.*;
import java.util.Arrays;

/**
 * Base functionality to extract a TAR file
 */
public class Tar {

    /**
     * Extract the given TAR file
     * @param sourceFile Path to the Tar file
     * @param destination Destination path for extraction
     * @throws IOException
     */
    public static void extract(String sourceFile, String destination) throws IOException {
        // create output directory if not exists
        File directory = new File(destination);
        if(!directory.exists() &&
                !directory.mkdirs()) {

            throw new IOException("Failed to create destination directory: " +
                    destination);
        }

        System.out.println("  Tar extract...");

        // get input stream
        FileInputStream in = new FileInputStream(sourceFile);

        // prepare buffer
        byte[] header = new byte[512];
        byte[] buffer = new byte[512];

        // read data
        int size;
        while ((size = in.read(header)) != -1) {
            if (size < 512 || isArrayEmpty(header)) {
                break;
            }

            // get type and file size
            int type = getOctal(header, 156, 1);
            long file_size = getOctalLong(header, 124, 12);

            // create out stream if entry is a file
            FileOutputStream out = null;
            if (type == 0) {
                // get file name
                String fileName =
                        directory.getPath() + "/" + getString(header, 0, 100);

                // get destination file
                File destinationFile = new File(fileName);

                // create parent directory of destination file if not exist
                if(!destinationFile.getParentFile().exists() &&
                        !destinationFile.getParentFile().mkdirs()) {

                    throw new IOException("Failed to create destination directory: " +
                            destinationFile.getParent());
                }

                // create stream
                out = new FileOutputStream(destinationFile);
            }

            // read TAR data
            for (int i = 0; i*512 < file_size; ++i) {
                int read = in.read(buffer, 0, 512);

                // stop if not enough data was read
                if (read < 512) {
                    throw new IOException("Invalid TAR file");
                }

                // write data to out stream if entry is a file
                if (out != null) {
                    // do not append extra "0"s
                    if (i*512 + 512 > file_size) {
                        out.write(buffer, 0, (int)file_size-i*512);
                    } else {
                        out.write(buffer);
                    }
                }
            }

            // close stream if opened
            if (out != null) {
                out.close();
            }
        }
        System.out.println("  Done!");
        in.close();
    }

    /**
     * Convert the data section to an integer
     * @param data Data to convert
     * @param from Start of section
     * @param length Length of section
     * @return Integer value of the section
     */
    private static int getOctal(byte[] data, int from, int length) {
        int num = 0;

        for (int i = 0; i < length; i++) {
            byte b = data[from+i];
            // only add numeric chars to return value
            if (b >= '0' && b <= '9') {
                num = num*8 + b-'0';
            }
        }
        return num;
    }

    /**
     * Convert the data section to an long integer
     * @param data Data to convert
     * @param from Start of section
     * @param length Length of section
     * @return Long integer value of the section
     */
    private static long getOctalLong(byte[] data, int from, int length) {
        long num = 0;

        for (int i = 0; i < length; i++) {
            // only add numeric chars to return value
            if (data[from+i] >= '0' && data[from+i] <= '9') {
                num = num*8 + data[from+i] - '0';
            }
        }
        return num;
    }

    /**
     * Convert the given section to a string
     * @param data Data to convert
     * @param from Start of section
     * @param length Length of section
     * @return String value of the section
     */
    private static String getString(byte[] data, int from, int length) {
        // prepare result variable
        byte[] arr = new byte[length];

        // start iteration
        boolean start = true;
        for (int i = 0; i < length; i++) {
            // stop if byte is '0' (line end)
            if (data[from+i] == 0) {
                arr = Arrays.copyOfRange(arr, 0, i);
                break;
            }
            // convert leading '0' to space
            if (start && data[from+i] == '0') {
                arr[i] = ' ';
            } else {
                start = false;
                arr[i] = data[from+i];
            }
        }
        // return string (remove spaces)
        return new String(arr).replaceAll(" ", "");
    }

    /**
     * Check if the given array is empty (only 0)
     * @param data Data to check
     * @return True if all elements are 0
     */
    private static boolean isArrayEmpty(byte[] data) {
        for (byte b : data) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}
