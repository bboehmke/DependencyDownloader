/*
 * Copyright (c) 2016 by Benjamin Böhmke
 *
 * DependencyDownloader is free software; you can redistribute it and/or modify it under
 * the terms of the MIT License. See the LICENSE file for more details.
 */

package net.boehmke.tools.dep_downloader;


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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

/**
 * Main class of the Dependency Downloader
 */
public class DependencyDownloader {
    /**
     * The entry point of application.
     *
     * @param args The commandline arguments
     */
    public static void main(String[] args) {
        // get and show version number
        String version = DependencyDownloader.class.getPackage().getImplementationVersion();
        System.out.println("Dependency Downloader - version " + version + "\n");

        // convert arguments to list
        List<String> argList = Arrays.asList(args);

        // set default settings
        boolean showHelp = false;
        boolean createChecksumMd5 = false;
        boolean createChecksumSha1 = false;
        String filePath = "depend.xml";
        String proxy = getProxySettings();

        // check if no arguments were given
        if (argList.size() > 0) {
            // get first argument
            String arg = argList.get(0);

            // help
            if (arg.equals("-h") || arg.equals("--help")) {
                showHelp = true;

            // generate MD5 checksum
            } else if (arg.equals("-m") || arg.equals("--createMd5")) {
                createChecksumMd5 = true;

                // check if file is given
                if (argList.size() < 2) {
                    showHelp = true;
                    System.err.println("[ERR] Checksum creation requires a path argument!\n");

                } else {
                    filePath = argList.get(1);
                }

            // generate SHA1 checksum
            } else if (arg.equals("-s") || arg.equals("--createSha1")) {
                createChecksumSha1 = true;

                // check if file is given
                if (argList.size() < 2) {
                    showHelp = true;
                    System.err.println("[ERR] Checksum creation requires a path argument!\n");

                } else {
                    filePath = argList.get(1);
                }

            // set proxy for download
            } else if (arg.equals("-p") || arg.equals("--proxy")) {
                // check if file is given
                if (argList.size() < 2) {
                    showHelp = true;
                    System.err.println("[ERR] Proxy requires a value!\n");

                } else {
                    // get proxy
                    proxy = argList.get(1);

                    // set file path if exist
                    if (argList.size() > 2) {
                        filePath = argList.get(2);
                    }
                }

            // unknown option
            } else if (arg.startsWith("-")) {
                showHelp = true;
                System.err.println("[ERR] Unknown Option: " + arg + "\n");

            } else {
                filePath = arg;
            }
        }

        if (showHelp) {
            showHelp();

        } else if (createChecksumMd5 || createChecksumSha1) {
            // check if source file exist
            File file = new File(filePath);
            if (!file.exists()) {
                // if not show error and help
                System.err.println("[ERR] Source file not found: " + filePath + "\n");
                showHelp();

            } else {
                // create and show checksum
                try {
                    if (createChecksumMd5) {
                        System.out.println("MD5 Checksum for " + filePath + ":");
                        System.out.println("  " + Checksum.createMd5(filePath));
                    } else {
                        System.out.println("SHA1 Checksum for " + filePath + ":");
                        System.out.println("  " + Checksum.createSha1(filePath));
                    }
                } catch (IOException e) {
                    System.err.println("=== ERROR ===");
                    System.err.println(e.getMessage());
                } catch (NoSuchAlgorithmException e) {
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
                showHelp();
            } else {

                // load dependency file
                try {
                    downloadDependencyList(filePath, proxy);
                } catch (IOException e) {
                    System.err.println("=== ERROR ===");
                    System.err.println(e.getMessage());
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("=== ERROR ===");
                    System.err.println(e.getMessage());
                } catch (SAXException e) {
                    System.err.println("=== ERROR ===");
                    System.err.println(e.getMessage());
                } catch (ParserConfigurationException e) {
                    System.err.println("=== ERROR ===");
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    /**
     * Show the commandline help
     */
    private static void showHelp() {
        String path = DependencyDownloader.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String baseName = path.substring(path.lastIndexOf('/')+1, path.length());
        System.out.println("===== Help =====");

        System.out.println("Command:");
        System.out.println("  " + baseName + " [OPTION] [FILE_PATH]\n");

        System.out.println("Options:");
        System.out.println("  -m --md5            generate MD5 hash of file");
        System.out.println("  -p --proxy [PROXY]  set path to proxy");
        System.out.println("  -s --sha1           generate SHA1 hash of file\n");

        System.out.println("Notes:");
        System.out.println("  If no file name is given the file \"depend.xml\" \n" +
                           "  will be used as dependency list.\n");
    }

    /**
     * Load the dependency list and download the dependencies
     * @param path Path to the dependency list
     * @param proxy Proxy used for download
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws NoSuchAlgorithmException
     */
    private static void downloadDependencyList(String path, String proxy) throws IOException,
            ParserConfigurationException, SAXException, NoSuchAlgorithmException {

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
        Element root = doc.getDocumentElement();

        // set tmp file
        String tmpFile = ".tmpDependencyFile.dat";

        // create downloader
        Downloader downloader = new Downloader(proxy);

        // get node list
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            // get node
            Node node = nodes.item(i);

            // if node is an element handle it
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;

                // handle normal files
                if (element.getTagName().equals("File")) {
                    System.out.println("=> Get plain file: " + element.getAttribute("Source"));
                    // download file
                    downloader.downloadFile(element.getAttribute("Source"),
                            element.getAttribute("Destination"));

                    // check checksum (if exist)
                    checkChecksum(element, downloader.getLastDownloadedFile());
                    System.out.println("");

                    // handle ZIP files
                } else if (element.getTagName().equals("Zip")) {
                    System.out.println("=> Get zip file: " + element.getAttribute("Source"));
                    // download file
                    downloader.downloadFile(element.getAttribute("Source"), tmpFile);

                    // check checksum (if exist)
                    checkChecksum(element, downloader.getLastDownloadedFile());

                    // decompress file
                    Zip.decompress(tmpFile,
                                   element.getAttribute("Destination"),
                                   element.getAttribute("SourceSubDir"));

                    // remove tmp file
                    Files.delete(Paths.get(tmpFile));

                    System.out.println("");

                // handle GZIP files
                } else if (element.getTagName().equals("GZip")) {
                    System.out.println("=> Get Gzip file: " + element.getAttribute("Source"));
                    // download file
                    downloader.downloadFile(element.getAttribute("Source"), tmpFile);

                    // check checksum (if exist)
                    checkChecksum(element, downloader.getLastDownloadedFile());

                    // decompress file
                    GZip.decompress(tmpFile, element.getAttribute("Destination"));

                    // remove tmp file
                    Files.delete(Paths.get(tmpFile));

                    System.out.println("");

                // handle TAR files
                } else if (element.getTagName().equals("Tar")) {
                    System.out.println("=> Get Tar file: " + element.getAttribute("Source"));
                    // download file
                    downloader.downloadFile(element.getAttribute("Source"), tmpFile);

                    // check checksum (if exist)
                    checkChecksum(element, downloader.getLastDownloadedFile());

                    // extract file
                    Tar.extract(tmpFile,
                                element.getAttribute("Destination"),
                                element.getAttribute("SourceSubDir"));

                    // remove tmp file
                    Files.delete(Paths.get(tmpFile));

                    System.out.println("");

                // handle TAR.GZ files
                } else if (element.getTagName().equals("TarGz")) {
                    System.out.println("=> Get TarGz file: " + element.getAttribute("Source"));
                    // download file
                    downloader.downloadFile(element.getAttribute("Source"), tmpFile);

                    // check checksum (if exist)
                    checkChecksum(element, downloader.getLastDownloadedFile());

                    // decompress file
                    GZip.decompress(tmpFile, tmpFile + ".ungz");

                    // extract file
                    Tar.extract(tmpFile + ".ungz",
                                element.getAttribute("Destination"),
                                element.getAttribute("SourceSubDir"));

                    // remove tmp file
                    Files.delete(Paths.get(tmpFile));
                    Files.delete(Paths.get(tmpFile + ".ungz"));

                    System.out.println("");
                } else {
                    System.err.println("Unknown file type: " + element.getNodeName());
                }
            }
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
}
