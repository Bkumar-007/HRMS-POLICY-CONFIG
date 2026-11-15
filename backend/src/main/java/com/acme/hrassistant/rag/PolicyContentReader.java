package com.acme.hrassistant.rag;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.rtf.RTFEditorKit;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Extracts plain text from uploaded policy files
 * (markdown / text / HTML / RTF / PDF / DOC / DOCX / ODT).
 */
@Component
public class PolicyContentReader {

    public String readText(MultipartFile file) {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        try {
            byte[] bytes = file.getBytes();
            if (filename.endsWith(".pdf")) {
                return requireText(readPdf(bytes), "PDF");
            }
            if (filename.endsWith(".docx")) {
                return requireText(readDocx(bytes), "DOCX");
            }
            if (filename.endsWith(".doc")) {
                return requireText(readDoc(bytes), "DOC");
            }
            if (filename.endsWith(".odt")) {
                return requireText(readOdt(bytes), "ODT");
            }
            if (filename.endsWith(".rtf")) {
                return requireText(readRtf(bytes), "RTF");
            }
            if (filename.endsWith(".html") || filename.endsWith(".htm")) {
                return requireText(stripHtml(new String(bytes, detectCharset(bytes))), "HTML");
            }
            // .md / .markdown / .txt and other plain text
            return requireText(new String(bytes, detectCharset(bytes)), "text");
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read upload: " + ex.getMessage(), ex);
        }
    }

    private static String readPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            if (document.getNumberOfPages() == 0) {
                throw new IllegalStateException("PDF has no pages");
            }
            return new PDFTextStripper().getText(document);
        }
    }

    private static String readDocx(byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes);
                XWPFDocument document = new XWPFDocument(in);
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String readDoc(byte[] bytes) throws IOException {
        try (InputStream in = new ByteArrayInputStream(bytes);
                HWPFDocument document = new HWPFDocument(in);
                WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String readOdt(byte[] bytes) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("content.xml".equals(entry.getName())) {
                    String xml = new String(zip.readAllBytes(), StandardCharsets.UTF_8);
                    return stripXml(xml);
                }
            }
        }
        throw new IllegalStateException("ODT file is missing content.xml");
    }

    private static String readRtf(byte[] bytes) throws IOException {
        try {
            RTFEditorKit kit = new RTFEditorKit();
            DefaultStyledDocument doc = new DefaultStyledDocument();
            kit.read(new ByteArrayInputStream(bytes), doc, 0);
            return doc.getText(0, doc.getLength());
        } catch (Exception ex) {
            throw new IOException("Could not parse RTF: " + ex.getMessage(), ex);
        }
    }

    private static String stripHtml(String html) {
        String withoutScripts = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        String withoutStyles = withoutScripts.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        return withoutStyles
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)</h[1-6]>", "\n")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n *", "\n")
                .trim();
    }

    private static String stripXml(String xml) {
        return xml.replaceAll("(?is)<text:p[^>]*>", "\n")
                .replaceAll("(?is)<text:h[^>]*>", "\n")
                .replaceAll("(?is)<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n *", "\n")
                .trim();
    }

    private static Charset detectCharset(byte[] bytes) {
        if (bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            return StandardCharsets.UTF_8;
        }
        // UTF-16 LE BOM
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return StandardCharsets.UTF_16LE;
        }
        return StandardCharsets.UTF_8;
    }

    private static String requireText(String text, String format) {
        if (text == null || text.isBlank()) {
            throw new IllegalStateException(
                    "Could not extract text from " + format + " (empty or image-only files are not supported)");
        }
        return text.trim();
    }
}
