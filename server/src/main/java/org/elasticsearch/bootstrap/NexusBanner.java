/*
 * Copyright © 2026 Yogoo Software Co., Ltd.
 * Licensed under the Apache License, Version 2.0.
 */

package org.elasticsearch.bootstrap;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.Build;
import org.elasticsearch.Version;

public final class NexusBanner {

    private static final String[] BANNER_LINES = {
        "",
        "  _   _                      _____ __  ___  ",
        " | \\ | | _____  ___   _ ___ |___  /_ |/ _ \\ ",
        " |  \\| |/ _ \\ \\/ / | | / __|   / / | | | | |",
        " | |\\  |  __/>  <| |_| \\__ \\  / /  | | |_| |",
        " |_| \\_|\\___/_/\\_\\\\__,_|___/ /_/   |_|\\___/ ",
        ""
    };

    private NexusBanner() {}

    public static void print(Logger logger) {
        for (String line : BANNER_LINES) {
            logger.info(line);
        }
        logger.info("  Nexus 710 v{} | Based on Elasticsearch {} | Lucene {}",
            Version.CURRENT, Build.CURRENT.getQualifiedVersion(),
            Version.CURRENT.luceneVersion);
        logger.info("");
    }
}
