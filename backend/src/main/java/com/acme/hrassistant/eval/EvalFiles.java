package com.acme.hrassistant.eval;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Locates files under the repo-level {@code eval/} folder regardless of whether the JVM
 * was started from the repo root or from {@code backend/}.
 */
public final class EvalFiles {

    private EvalFiles() {
    }

    public static Path resolve(String fileName) {
        List<Path> candidates = List.of(
                Path.of("eval", fileName),
                Path.of("..", "eval", fileName),
                Path.of(System.getProperty("user.dir"), "eval", fileName),
                Path.of(System.getProperty("user.dir"), "..", "eval", fileName));
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        throw new IllegalStateException("eval/" + fileName + " not found from " + System.getProperty("user.dir"));
    }

    /** {@code eval/results/<fileName>} next to the dataset files (directory is created when missing). */
    public static Path resultsFile(String fileName) throws IOException {
        Path evalDir = resolve("agent-eval.json").getParent();
        Path results = evalDir.resolve("results");
        Files.createDirectories(results);
        return results.resolve(fileName);
    }

    public static void write(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    public static String markdownCell(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "\\|").replace("\r", " ").replace("\n", " ").trim();
    }
}
