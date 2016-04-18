/*
 * Copyright (c) 2016 by Benjamin Böhmke
 *
 * DependencyDownloader is free software; you can redistribute it and/or modify it under
 * the terms of the MIT License. See the LICENSE file for more details.
 */

package net.boehmke.tools.dependency_downloader.cli;

/**
 * Simple Parser Exception
 */
public class ParserException extends Exception {
    /**
     * Create new exception
     * @param msg Message of the exception
     */
    public ParserException(String msg) {
        super(msg);
    }
}
