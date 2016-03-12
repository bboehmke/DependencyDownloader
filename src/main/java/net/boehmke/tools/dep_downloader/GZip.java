/*
 * Copyright (c) 2016 by Benjamin Böhmke
 *
 * DependencyDownloader is free software; you can redistribute it and/or modify it under
 * the terms of the MIT License. See the LICENSE file for more details.
 */

package net.boehmke.tools.dep_downloader;


import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Base functionality to decompress a GZIP file
 */
public class GZip {

    /**
     * Decompress the given GZIP file
     * @param sourceFile Path to the GZIP file
     * @param destination Destination path for decompression
     * @throws IOException
     */
    public static void decompress(String sourceFile, String destination) throws IOException {
        System.out.println("  GZip decompress...");

        // get input stream
        GZIPInputStream gzip =
                new GZIPInputStream(new FileInputStream(sourceFile));

        // get output stream
        FileOutputStream out = new FileOutputStream(destination);

        // prepare buffer
        byte[] buffer = new byte[1024*10];

        // read data
        int size;
        while ((size = gzip.read(buffer)) != -1) {
            // write data to file
            out.write(buffer, 0, size);
        }
        System.out.println("  Done!");

        // close streams
        gzip.close();
        out.close();
    }
}
