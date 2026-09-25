/*******************************************************************************
 * The contents of this file are subject to the Common Public Attribution License
 * Version 1.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.projectlibre.com/license
 *******************************************************************************/
package com.projectlibre1.util;

import java.io.ObjectInputFilter;

/**
 * Restricts Java deserialization to ProjectLibre data and the JDK value types
 * used by that data. Project files are user-controlled input and must not be
 * allowed to instantiate arbitrary classes.
 */
public final class SerializationFilter {
    private static final long MAX_DEPTH = 100;
    private static final long MAX_REFERENCES = 200000;
    private static final long MAX_STREAM_BYTES = 512L * 1024 * 1024;
    private static final long MAX_ARRAY_LENGTH = 67_108_864L;

    private static final ObjectInputFilter INSTANCE = info -> {
        if (info.depth() > MAX_DEPTH || info.references() > MAX_REFERENCES || info.streamBytes() > MAX_STREAM_BYTES) {
            return ObjectInputFilter.Status.REJECTED;
        }
        if (info.arrayLength() >= 0 && info.arrayLength() > MAX_ARRAY_LENGTH) {
            return ObjectInputFilter.Status.REJECTED;
        }

        Class<?> serialClass = info.serialClass();
        if (serialClass == null) {
            // Null for dynamic proxies but also for array type descriptors
            // (e.g. the byte[] magnitude inside BigDecimal, which Money
            // extends). No InvocationHandler exists in the allowed packages,
            // so proxies cannot resolve to anything usable; leave undecided
            // rather than break legitimate money/cost data.
            return ObjectInputFilter.Status.UNDECIDED;
        }

        String className = serialClass.getName();
        while (className.startsWith("[")) {
            className = className.substring(1);
            if (className.startsWith("L") && className.endsWith(";")) {
                className = className.substring(1, className.length() - 1);
            }
        }

        if (className.length() == 1 && "ZBCSIJFD".indexOf(className.charAt(0)) >= 0) {
            return ObjectInputFilter.Status.ALLOWED;
        }

        if (className.equals("java.lang.Class")
                || className.equals("java.lang.ClassLoader")
                || className.equals("java.lang.Process")
                || className.equals("java.lang.ProcessBuilder")
                || className.equals("java.lang.Runtime")
                || className.equals("java.lang.Thread")
                || className.equals("java.lang.ThreadGroup")
                || className.startsWith("java.lang.invoke.")
                || className.startsWith("java.lang.management.")
                || className.startsWith("java.lang.ref.")
                || className.startsWith("java.lang.reflect.")
                || className.startsWith("java.util.concurrent.")
                || className.startsWith("java.util.jar.")
                || className.startsWith("java.util.logging.")
                || className.startsWith("java.util.prefs.")
                || className.startsWith("java.util.spi.")
                || className.equals("java.util.ServiceLoader")) {
            return ObjectInputFilter.Status.REJECTED;
        }

        if (className.startsWith("com.projectlibre1.")
                || className.startsWith("com.projectlibre.")
                || className.startsWith("net.sf.mpxj.")
                || className.startsWith("org.projectlibre.")
                || className.startsWith("java.awt.print.")
                || className.startsWith("java.lang.")
                || className.startsWith("java.math.")
                || className.startsWith("java.time.")
                || className.startsWith("java.util.")
                || className.startsWith("javax.print.attribute.")
                || className.equals("java.io.File")
                || className.equals("java.net.URI")) {
            return ObjectInputFilter.Status.ALLOWED;
        }

        return ObjectInputFilter.Status.REJECTED;
    };

    private SerializationFilter() {
    }

    public static ObjectInputFilter get() {
        return INSTANCE;
    }
}
