package com.javokhir;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ConcurrentWordFileIndexer implements FileIndexer, DirectoryIndexer {
    private final Map<String, Set<Path>> wordToFileMap = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);
    private final CompletionService<Void> completionService = new ExecutorCompletionService<>(executorService);
    private final AtomicInteger taskCount = new AtomicInteger(0);

    private final String WORD_SPLITTER = "\\s+";
    private final boolean isDebug;

    public ConcurrentWordFileIndexer(boolean isDebug) {
        this.isDebug = isDebug;
    }

    public ConcurrentWordFileIndexer() {
        isDebug = false;
    }

    public void indexDirectory(Path path) {
        try {
            completionService.submit(() -> traverseAndIndex(path));
            taskCount.incrementAndGet();

            for (int i = 0; i < taskCount.get(); i++) {
                completionService.take().get();
            }
        } catch (Exception e) {
            if(isDebug) {
                System.err.println("Failed to index directory: " + path);
            }
        }
    }

    private Void traverseAndIndex(Path path) throws IOException {

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (dir.toString().equals(path.toString())) {
                    return FileVisitResult.CONTINUE;
                }

                completionService.submit(() -> traverseAndIndex(dir));
                taskCount.incrementAndGet();
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                completionService.submit(() -> {
                    indexFile(file);
                    return null;
                });
                taskCount.incrementAndGet();
                return FileVisitResult.CONTINUE;
            }
        });
        return null;
    }

    public void indexFile(Path file) {
        try (BufferedReader bufferedReader = Files.newBufferedReader(file)) {
            bufferedReader
                    .lines()
                    .parallel()
                    .flatMap(line -> Arrays.stream(line.split(WORD_SPLITTER)))
                    .map(String::toLowerCase)
                    .forEach(word -> wordToFileMap.computeIfAbsent(word, k -> ConcurrentHashMap.newKeySet()).add(file));
        } catch (Exception e) {
            if(isDebug) {
                System.err.println("Failed to index file: " + file);
            }
        }
    }

    public Set<Path> searchWord(String word) {
        if(word == null) return Collections.emptySet();

        return wordToFileMap.getOrDefault(word.toLowerCase(), Collections.emptySet());
    }
}
