/*
 * Copyright (c) 2016 by Benjamin Böhmke
 *
 * DependencyDownloader is free software; you can redistribute it and/or modify it under
 * the terms of the MIT License. See the LICENSE file for more details.
 */

package net.boehmke.tools.dep_downloader;


import java.io.*;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Base functionality to decompress a ZIP file
 */
public class Zip {

    /**
     * Decompress the given ZIP file
     * @param sourceFile Path to the ZIP file
     * @param destination Destination path for decompression
     * @throws IOException
     */
    public static void decompress(String sourceFile, String destination) throws IOException {
        // create output directory if not exists
        File directory = new File(destination);
        if(!directory.exists() &&
           !directory.mkdirs()) {

            throw new IOException("Failed to create destination directory: " +
                    destination);
        }

        // get ZIP file
        ZipFile zipFile = new ZipFile(sourceFile);

        // get entry list
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        // get amount of files in ZIP file
        int fileCount = zipFile.size();

        System.out.println("  Zip decompress (~" + fileCount + " files)...");

        // actual extracted size
        int extractedFiles = 0;

        // prepare buffer
        byte[] buffer = new byte[1024*10];

        while(entries.hasMoreElements()){
            // get nex entry
            ZipEntry entry = entries.nextElement();

            // skip directories
            if (entry.isDirectory()) {
                ++extractedFiles;
                continue;
            }

            // get file name
            String fileName = entry.getName();

            // get destination file
            File destinationFile = new File(destination + File.separator + fileName);

            // create parent directory of destination file if not exist
            if(!destinationFile.getParentFile().exists() &&
               !destinationFile.getParentFile().mkdirs()) {

                throw new IOException("Failed to create destination directory: " +
                        destinationFile.getParent());
            }

            // stream for destination file
            FileOutputStream out = new FileOutputStream(destinationFile);

            // stream for ZIP entry
            InputStream in = zipFile.getInputStream(entry);

            // read data
            int size;
            while ((size = in.read(buffer)) != -1) {
                // write data to file
                out.write(buffer, 0, size);
            }

            // close destination file stream
            out.close();


            // update progress if changed
            ++extractedFiles;
            System.out.print("\r  " + extractedFiles + "/" + fileCount);
        }
        System.out.println("\r  Done!");
    }
}
