/*
 * Copyright (c) 2016 by Benjamin Böhmke
 *
 * DependencyDownloader is free software; you can redistribute it and/or modify it under
 * the terms of the MIT License. See the LICENSE file for more details.
 */

package net.boehmke.tools.dependency_downloader;

import net.boehmke.tools.dependency_downloader.cli.Parser;

import net.boehmke.tools.dependency_downloader.cli.ParserException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;

/**
 * Main class of the Dependency Downloader
 */
public class DependencyDownloader {
    /**
     * Location of download cache
     */
    private static final String cachePath = ".dependencyDownloader/";

    /**
     * The entry point of application.
     *
     * @param args The commandline arguments
     */
    public static void main(String[] args) throws ParserException {
        // get and show version number
        String version = DependencyDownloader.class.getPackage().getImplementationVersion();
        System.out.println("Dependency Downloader - version " + version + "\n");

        // create commandline parser and add options and parameter
        Parser parser = new Parser();

        parser.addOption("clear-cache", null, false, "Removes the cache after extraction");
        parser.addOption("clean", null, false, "Cleanup previous downloaded dependencies");
        parser.addOption("download-only", null, false, "Only download the dependencies to the cache");
        parser.addOption("help", "h", false, "Show this help");
        parser.addOption("md5", "m", false, "Generate MD5 hash of file");
        parser.addOption("proxy", "p", true, "Set path to proxy");
        parser.addOption("sha1", "s", false, "Generate SHA1 hash of file");

        parser.addParameter("FILE", "Path to the file (Default: \"depend.xml\")");

        try {
            // handle commandline arguments
            parser.handle(args);

        } catch (ParserException e) {
            System.err.println("=== ERROR ===");
            System.err.println(e.getMessage());
            System.err.println();
            parser.showHelp();
            System.exit(1);
        }

        // get some arguments values
        String filePath = parser.getValue("FILE", "depend.xml");
        String proxy = parser.getValue("proxy", getProxySettings());

        if (parser.isSet("help")) {
            parser.showHelp();

        } else {
            if (parser.isSet("md5") || parser.isSet("sha1")) {
                // check if source file exist
                File file = new File(filePath);
                if (!file.exists()) {
                    // if not show error and help
                    System.err.println("[ERR] Source file not found: " + filePath + "\n");
                    parser.showHelp();

                } else {
                    // create and show checksum
                    try {
                        if (parser.isSet("md5")) {
                            System.out.println("MD5 Checksum for " + filePath + ":");
                            System.out.println("  " + Checksum.createMd5(filePath));
                        }
                        if (parser.isSet("sha1")) {
                            System.out.println("SHA1 Checksum for " + filePath + ":");
                            System.out.println("  " + Checksum.createSha1(filePath));
                        }
                    } catch (IOException | NoSuchAlgorithmException e) {
                        System.err.println("=== ERROR ===");
                        System.err.println(e.getMessage());
                    }
                }

            } else {
                // check if default dependency file exist
                File file = new File(filePath);
                if (!file.exists()) {
                    // if not show error and help
                    System.err.println("[ERR] Dependency file not found: " + filePath + "\n");
                    parser.showHelp();
                } else {

                    // load dependency file
                    try {
                        handleDependencies(
                                filePath, proxy,
                                parser.isSet("clean"),
                                parser.isSet("download-only"),
                                parser.isSet("clear-cache"));

                    } catch (IOException | NoSuchAlgorithmException | SAXException | ParserConfigurationException e) {
                        System.err.println("=== ERROR ===");
                        System.err.println(e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * Handle the Dependency list
     * @param dependFilePath Path to dpend file
     * @param proxy Proxy setting for download
     * @param clean Remove existing (extracted) dependencies
     * @param onlyDownload Only download dependencies (no extract)
     * @param clearCache Clear the cache after download
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private static void handleDependencies(String dependFilePath, String proxy,
                                           boolean clean, boolean onlyDownload,
                                           boolean clearCache)
            throws ParserConfigurationException, SAXException, IOException,
                   NoSuchAlgorithmException {

        // create downloader
        Downloader downloader = new Downloader(proxy);

        // get node list
        NodeList nodes = loadDependencyList(dependFilePath).getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            // get node
            Node node = nodes.item(i);

            // if node is an element handle it
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                // cleanup
                if (clean) {
                    deleteDependency(element);

                // download & extract
                } else {
                    System.out.println("=> Handle " + element.getTagName() +
                            " file: " + element.getAttribute("Source"));

                    // download and/or check file
                    String filePath = downloadCheckDependency(element, downloader);

                    // extract if allowed
                    if (!onlyDownload) {
                        extractDependency(element, filePath);
                    }
                }
            }
        }

        // delete cache if requested
        if (clearCache) {
            deleteDir(cachePath);
        }

    }

    /**
     * Load and validate the dependency list
     * @param path Path to the depend file
     * @return Root element of the file
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     */
    private static Element loadDependencyList(String path)
            throws ParserConfigurationException, SAXException, IOException {

        // load XSD file
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source schemaSource = new StreamSource(
                DependencyDownloader.class.getResourceAsStream("/depend.xsd"));
        Schema schema = schemaFactory.newSchema(schemaSource);

        // get XML factory and doc builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder docBuilder = factory.newDocumentBuilder();

        // parse document
        Document doc = docBuilder.parse(path);

        // validate schema
        schema.newValidator().validate(new DOMSource(doc));

        // get root element
        return doc.getDocumentElement();
    }

    /**
     * Load the dependency list and clean all destinations
     * @param element Element of dependency
     */
    private static void deleteDependency(Element element) {
        System.out.println("=> Remove " + element.getAttribute("Destination"));

        // delete destination
        try {
            deleteDir(element.getAttribute("Destination"));
        } catch (IOException e) {
            System.out.println("  Already removed!");
        }
    }

    /**
     * Load the dependency list and clean all destinations
     * @param element Element of dependency
     * @param downloader Downloader instance
     * @return Path to cache file
     */
    private static String downloadCheckDependency(Element element, Downloader downloader)
            throws NoSuchAlgorithmException, IOException {
        String source = element.getAttribute("Source");
        String sourceBaseName = source.substring(source.lastIndexOf('/')+1, source.length());

        // get path for cache file
        String cacheFilePath = cachePath + sourceBaseName;

        // check if file is in cache
        if (new File(cacheFilePath).exists()) {
            System.out.println("  -> Found file in cache!");
            try {
                // check checksum (if exist)
                checkChecksum(element, cacheFilePath);

                // file exist and checksum is valid or missing -> use cache
                return cacheFilePath;
            } catch (IOException e) {
                System.out.println("  Checksum of cached file is invalid! Try redownload!");
            }
        }

        // download the file
        downloader.downloadFile(element.getAttribute("Source"), cachePath + "tmp.dat");

        // check checksum (if exist)
        checkChecksum(element, cachePath + "tmp.dat");

        // copy file to cache
        Files.copy(new File(cachePath + "tmp.dat").toPath(),
                   new File(cacheFilePath).toPath());

        // remove tmp file
        Files.delete(Paths.get(cachePath + "tmp.dat"));

        return cacheFilePath;
    }

    /**
     * Extract the dependency
     * @param element Element to extract
     * @param filePath Path of the cache file
     * @throws IOException
     */
    private static void extractDependency(Element element, String filePath)
            throws IOException {
        // handle normal files
        if (element.getTagName().equals("File")) {
            System.out.println("  Copy plain file: " + filePath);

            // copy the file
            Files.copy(new File(filePath).toPath(),
                       new File(element.getAttribute("Destination")).toPath());

            System.out.println("");

            // handle ZIP files
        } else if (element.getTagName().equals("Zip")) {
            System.out.println("  Extract zip file: " + filePath);

            // decompress file
            Zip.decompress(filePath,
                           element.getAttribute("Destination"),
                           element.getAttribute("SourceSubDir"));

            System.out.println("");

            // handle GZIP files
        } else if (element.getTagName().equals("GZip")) {
            System.out.println("  Decompress Gzip file: " + filePath);

            // decompress file
            GZip.decompress(filePath, element.getAttribute("Destination"));

            System.out.println("");

            // handle TAR files
        } else if (element.getTagName().equals("Tar")) {
            System.out.println("  Extract Tar file: " + filePath);

            // extract file
            Tar.extract(filePath,
                        element.getAttribute("Destination"),
                        element.getAttribute("SourceSubDir"));

            System.out.println("");

            // handle TAR.GZ files
        } else if (element.getTagName().equals("TarGz")) {
            System.out.println("  Decompress TarGz file: " + filePath);

            // decompress file
            GZip.decompress(filePath, cachePath + "tmp.dat");

            System.out.println("  Extract TarGz file: " + filePath);

            // extract file
            Tar.extract(cachePath + "tmp.dat",
                        element.getAttribute("Destination"),
                        element.getAttribute("SourceSubDir"));

            // remove tmp file
            Files.delete(Paths.get(cachePath + "tmp.dat"));

            System.out.println("");
        } else {
            System.err.println("Unknown file type: " + element.getNodeName());
        }
    }

    /**
     * Check if the element has a Checksum and check it if exist
     * @param element Dom element (maybe) with checksum
     * @param path Path to the downloaded file
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private static void checkChecksum(Element element, String path)
            throws IOException, NoSuchAlgorithmException {

        // check MD5
        if (element.hasAttribute("Md5")) {
            Checksum.checkMd5(path, element.getAttribute("Md5"));
        }
        // check SHA1
        if (element.hasAttribute("Sha1")) {
            Checksum.checkSha1(path, element.getAttribute("Sha1"));
        }
    }

    /**
     * Get the proxy setting of the system
     * @return Proxy setting or empty string
     */
    private static String getProxySettings() {
        String http_proxy = System.getenv("http_proxy");
        if (http_proxy == null) {
            http_proxy = System.getenv("HTTP_PROXY");
        }
        // if no proxy settings found -> empty string
        if (http_proxy != null) {
            return http_proxy;
        } else {
            return "";
        }
    }

    /**
     * Delete directory and all files in it
     * @param path Path to the directory
     */
    private static void deleteDir(String path) throws IOException {
        Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
