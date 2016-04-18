/*
 * Copyright (c) 2016 by Benjamin Böhmke
 *
 * DependencyDownloader is free software; you can redistribute it and/or modify it under
 * the terms of the MIT License. See the LICENSE file for more details.
 */

package net.boehmke.tools.dependency_downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * Simple HTTP download functionality
 */
public class Downloader {
    /**
     * Proxy for download
     */
    private Proxy proxy;

    /**
     * Create the downloader
     * @param proxyUrl Proxy URL (Auth not supported)
     */
    public Downloader(String proxyUrl) {
        // if proxy is empty -> no proxy
        if (proxyUrl.isEmpty()) {
            proxy = Proxy.NO_PROXY;
        } else {
            // convert and split proxy url
            proxyUrl = proxyUrl.replaceAll("http[s]*://", "");
            String address = proxyUrl.substring(0, proxyUrl.lastIndexOf(':'));
            String port = proxyUrl.substring(proxyUrl.lastIndexOf(':')+1, proxyUrl.length());

            try {
                proxy = new Proxy(Proxy.Type.HTTP,
                            new InetSocketAddress(
                                    InetAddress.getByName(address),
                                    Integer.parseInt(port)));

            // Error -> no Proxy
            } catch (UnknownHostException e) {
                proxy = Proxy.NO_PROXY;
            }
        }
    }

    /**
     * Download the given file
     * @param source Source URL
     * @param destination Destination file or folder
     * @throws IOException
     */
    public void downloadFile(String source, String destination) throws IOException {
        // get file object for destination file
        File destinationFile = new File(destination);

        // convert string to URL object
        URL url = new URL(source);

        // send request to server
        HttpURLConnection request = (HttpURLConnection)url.openConnection(proxy);
        // enable redirect
        request.setInstanceFollowRedirects(true);

        // check if the destination is a directory
        if (destination.endsWith("/")) {
            // get the filename from source url
            String filename = source.substring(source.lastIndexOf('/')+1, source.length());

            // set new destination file
            destinationFile = new File(destination + filename);
        }

        // create parent directory ifg not exist
        if (destinationFile.getParentFile() != null &&
            !destinationFile.getParentFile().exists() &&
            !destinationFile.getParentFile().mkdirs()) {

            throw new IOException("Failed to create destination directory: " +
                    destinationFile.getParent());
        }

        // get out stream
        FileOutputStream out = new FileOutputStream(destinationFile);

        // get the size of the content
        long fileSize = request.getContentLengthLong();
        System.out.println("  Download (Size " + convertSize(fileSize) + ")...");

        // check response code of request
        if (request.getResponseCode() != 200) {
            throw new IOException("Bad response: " + request.getResponseMessage());
        }

        // get in stream
        InputStream in = request.getInputStream();

        // prepare buffer
        byte[] buffer = new byte[1024*10];

        // already loaded data size
        long loadedSize = 0;

        // last shown progress
        long lastProgress = -1;

        // read data
        int size;
        while ((size = in.read(buffer)) != -1) {
            // write data to file
            out.write(buffer, 0, size);

            // add size to loaded data size
            loadedSize += size;

            // calculate progress
            long pro = loadedSize*100/fileSize;

            // update progress if changed
            if (pro > lastProgress) {
                lastProgress = pro;
                System.out.print("\r  " + pro + "%");
            }
        }
        System.out.println("\r  Done!");

        // close streams
        in.close();
        out.close();
    }

    /**
     * Convert the given size in a string
     * @param size Size to convert
     * @return Size as string
     */
    private String convertSize(long size) {
        // lower than 2KB -> show as byte
        if (size < 1024*2) {
            return size + " B";

        } else {
            // convert to KB
            size = size/1024;

            // lower than 2MB -> show as KB
            if (size < 1024*2) {
                return size + " KB";

            } else {
                // convert to MB
                size = size/1024;

                // lower than 2GB -> show as MB
                if (size < 1024*2) {
                    return size + " MB";

                } else {
                    // convert to GB
                    size = size/1024;

                    // show GB
                    return size + " GB";
                }
            }
        }
    }
}
