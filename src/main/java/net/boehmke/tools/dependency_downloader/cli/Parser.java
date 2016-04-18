/*
 * Copyright (c) 2016 by Benjamin Böhmke
 *
 * DependencyDownloader is free software; you can redistribute it and/or modify it under
 * the terms of the MIT License. See the LICENSE file for more details.
 */

package net.boehmke.tools.dependency_downloader.cli;

import net.boehmke.tools.dependency_downloader.DependencyDownloader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

/**
 * Basic commandline parser
 */
public class Parser {
    /**
     * Commandline entry
     */
    private class Entry {
        /**
         * If true entry is an option
         */
        boolean isOption;
        /**
         * If true option has a value (option only)
         */
        boolean hasValue;
        /**
         * Name of the entry
         */
        String name;
        /**
         * Short name of the entry (option only)
         */
        String shortName;
        /**
         * Description of the entry
         */
        String description;
        /**
         * Value of the entry
         */
        String value;
    }

    /**
     * List with added entries
     */
    private List<Entry> entries = new ArrayList<>();

    /**
     * Add a new option
     * @param name Name of the option
     * @param shortName Short name of the option or null
     * @param hasValue True if the option has a value
     * @param description Description of the option
     */
    public void addOption(String name, String shortName, boolean hasValue, String description) {
        Entry entry = new Entry();
        entry.isOption = true;
        entry.hasValue = hasValue;
        entry.name = name;
        entry.shortName = shortName;
        entry.description = description;
        entry.value = null;
        entries.add(entry);
    }

    /**
     * Add a new parameter
     * @param name Name of the parameter
     * @param description Description of the parameter
     */
    public void addParameter(String name, String description) {
        Entry entry = new Entry();
        entry.isOption = false;
        entry.name = name;
        entry.description = description;
        entry.value = null;
        entries.add(entry);
    }

    /**
     * Handle the commandline
     * @param args List with commandline elements
     * @throws ParserException
     */
    public void handle(String[] args) throws ParserException {
        // convert arguments to list
        ListIterator<String> argList = Arrays.asList(args).listIterator();

        // handle each argument
        boolean parameterSection = false;
        while (argList.hasNext()) {
            String arg = argList.next();

            // hanlde options
            if (!parameterSection && arg.startsWith("-")) {
                // remove -
                arg = arg.substring(1);

                boolean shortName = true;

                // check if long name
                if (arg.startsWith("-")) {
                    // remove the 2nd '-'
                    arg = arg.substring(1);

                    shortName = false;
                }

                // find option in list
                boolean found = false;
                for (Entry entry: entries) {
                    if (entry.isOption) {
                        // check name or short name
                        if ((shortName && entry.shortName != null && entry.shortName.equals(arg)) ||
                            entry.name.equals(arg)) {
                            found = true;

                            // set value if option has one
                            if (entry.hasValue) {
                                if (!argList.hasNext()) {
                                    throw new ParserException("Unknown Option: " + arg);
                                }
                                entry.value = argList.next();
                            } else {
                                entry.value = "true";
                            }

                            break;
                        }
                    }
                }
                // error if no option found
                if (!found) {
                    throw new ParserException("Unknown Option: " + arg);
                }

            } else {
                parameterSection = true;

                // search parameter
                boolean found = false;
                for (Entry entry: entries) {
                    if (!entry.isOption) {
                        if (entry.value == null) {
                            found = true;
                            entry.value = arg;
                            break;
                        }
                    }
                }
                // error if parameter not found
                if (!found) {
                    throw new ParserException("Unknown Parameter: " + arg);
                }
            }
        }
    }

    /**
     * Print spaces to stdout
     * @param amount Amount of spaces
     */
    private void printSpaces(int amount) {
        for (int i = 0; i < amount; ++i) {
            System.out.print(" ");
        }
    }

    /**
     * Show command help
     */
    public void showHelp() {
        String path = DependencyDownloader.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String baseName = path.substring(path.lastIndexOf('/')+1, path.length());
        System.out.println("===== Help =====");

        // build parameter list
        String parameter = "";
        int parameterMaxSize = 0;
        int optionMaxSize = 0;
        for (Entry entry: entries) {
            if (!entry.isOption) {
                parameter += " " + entry.name;
                parameterMaxSize = Math.max(parameterMaxSize, entry.name.length());
            } else {
                if (entry.hasValue) {
                    optionMaxSize = Math.max(optionMaxSize, entry.name.length() + 8);
                } else {
                    optionMaxSize = Math.max(optionMaxSize, entry.name.length());
                }

            }
        }

        // print usage help
        System.out.println("Command:");
        System.out.println("  " + baseName + " [OPTION]" + parameter + "\n");

        // print option list
        if (optionMaxSize > 0) {
            System.out.println("Options:");
            for (Entry entry: entries) {
                if (entry.isOption) {
                    // show short name if exist
                    if (entry.shortName != null) {
                        System.out.print("  -" + entry.shortName + ", --");
                    } else {
                        System.out.print("      --");
                    }
                    System.out.print(entry.name);
                    if (entry.hasValue) {
                        System.out.print(" [VALUE]");
                        printSpaces(optionMaxSize - entry.name.length() - 6);
                    } else {
                        printSpaces(optionMaxSize - entry.name.length() + 2);
                    }
                    System.out.println(entry.description);
                }
            }
            System.out.println();
        }

        // print parameter list
        if (parameterMaxSize > 0) {
            System.out.println("Parameter:");
            for (Entry entry: entries) {
                if (!entry.isOption) {
                    System.out.print("  " + entry.name);
                    printSpaces(parameterMaxSize - entry.name.length() + 2);
                    System.out.println(entry.description);
                }
            }
            System.out.println();
        }
    }

    /**
     * Check if the entry is set
     * @param name Name of the entry
     * @return True if set
     * @throws ParserException
     */
    public boolean isSet(String name) throws ParserException {
        for (Entry entry: entries) {
            if (entry.name.equals(name)) {
                return entry.value != null;
            }
        }
        throw new ParserException("Invalid CLI Entry: " + name);
    }

    /**
     * Get the value of a entry
     * @param name Name of the entry
     * @param def Default value of the entry (if not set)
     * @return Value of the entry or default value
     * @throws ParserException
     */
    public String getValue(String name, String def) throws ParserException {
        for (Entry entry: entries) {
            if (entry.name.equals(name)) {
                if (entry.value == null) {
                    return def;
                } else {
                    return entry.value;
                }
            }
        }
        throw new ParserException("Invalid CLI Entry: " + name);
    }

}
