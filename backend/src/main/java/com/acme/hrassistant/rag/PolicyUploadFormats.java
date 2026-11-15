package com.acme.hrassistant.rag;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Supported policy upload extensions ↔ MIME types. */
public final class PolicyUploadFormats {

    private static final Map<String, String> EXTENSION_TO_MIME = new LinkedHashMap<>();

    static {
        EXTENSION_TO_MIME.put(".md", "text/markdown");
        EXTENSION_TO_MIME.put(".markdown", "text/markdown");
        EXTENSION_TO_MIME.put(".txt", "text/plain");
        EXTENSION_TO_MIME.put(".html", "text/html");
        EXTENSION_TO_MIME.put(".htm", "text/html");
        EXTENSION_TO_MIME.put(".rtf", "application/rtf");
        EXTENSION_TO_MIME.put(".pdf", "application/pdf");
        EXTENSION_TO_MIME.put(".doc", "application/msword");
        EXTENSION_TO_MIME.put(
                ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        EXTENSION_TO_MIME.put(".odt", "application/vnd.oasis.opendocument.text");
    }

    public static final String DEFAULT_ALLOWED_TYPES = String.join(
            ",",
            "text/markdown",
            "text/plain",
            "text/html",
            "application/rtf",
            "text/rtf",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.oasis.opendocument.text");

    private PolicyUploadFormats() {
    }

    public static Set<String> supportedExtensions() {
        return EXTENSION_TO_MIME.keySet();
    }

    public static boolean isSupportedExtension(String filename) {
        return mimeForFilename(filename) != null;
    }

    public static String mimeForFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return null;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : EXTENSION_TO_MIME.entrySet()) {
            if (lower.endsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static String humanList() {
        return String.join(", ", EXTENSION_TO_MIME.keySet());
    }
}
