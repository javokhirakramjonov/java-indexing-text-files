package com.javokhir;

import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ConcurrentWordFileIndexerTest {

    private static final String TEST_DATA_PATH = "test_data";
    private static final Integer INNER_DIR_COUNT = 10;
    private static final Integer DUMMY_FILES_COUNT = 10;

    private final ConcurrentWordFileIndexer indexer = new ConcurrentWordFileIndexer();

    @BeforeEach
    public void setup() {
        System.out.println("Test setup created: " + Paths.get(TEST_DATA_PATH).toFile().mkdir());
    }

    @AfterEach
    public void cleanup() {
        System.out.println("Cleaned up: " + deleteFolder(Paths.get(TEST_DATA_PATH).toFile()));
    }

    public static boolean deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) { //some JVMs return null for empty directories
            for (File file : files) {
                deleteFolder(file);
            }
        }

        return folder.delete();
    }

    @Test
    public void searchWord_fromFile_success() throws IOException {
        var wordToWrite = "Something";
        var filePath = createDummyFileWithWord(wordToWrite);

        indexer.indexFile(filePath);
        var paths = indexer.searchWord(wordToWrite);

        Assertions.assertEquals(1, paths.size());
        Assertions.assertEquals(filePath, paths.stream().findFirst().get());
    }

    @Test
    public void searchWord_fromDirectory_success() throws IOException {
        var wordToWrite = "Something";
        var filePath = createDummyFileWithWord(wordToWrite);

        indexer.indexDirectory(Paths.get(TEST_DATA_PATH));
        var paths = indexer.searchWord(wordToWrite);

        Assertions.assertEquals(1, paths.size());
        Assertions.assertEquals(filePath, paths.stream().findFirst().get());
    }

    @Test
    public void searchWord_fromFile_fail() throws IOException {
        var wordToWrite = "Something";
        var filePath = createDummyFileWithWord(wordToWrite);

        indexer.indexFile(filePath);
        var paths = indexer.searchWord("SomethingDifferent");

        Assertions.assertTrue(paths.isEmpty());
    }

    @Test
    public void searchWord_fromDirectory_fail() throws IOException {
        var wordToWrite = "Something";
        var filePath = createDummyFileWithWord(wordToWrite);

        indexer.indexDirectory(filePath);
        var paths = indexer.searchWord("SomethingDifferent");

        Assertions.assertTrue(paths.isEmpty());
    }

    @Test
    public void searchWord_fromFiles_multipleResult() {
        var wordToWrite = "Something";
        var filePaths = createDummyFilesWithWord(wordToWrite);
        for (Path path : filePaths) {
            indexer.indexFile(path);
        }

        var foundPaths = indexer.searchWord(wordToWrite);

        Assertions.assertEquals(filePaths.size(), foundPaths.size());
        Assertions.assertEquals(new HashSet<>(filePaths), foundPaths);
    }

    @Test
    public void searchNull_emptyResult() throws IOException {
        var wordToWrite = "Something";
        var filePath = createDummyFileWithWord(wordToWrite);
        indexer.indexFile(filePath);

        var paths = indexer.searchWord(null);

        Assertions.assertTrue(paths.isEmpty());
    }

    @Test
    void searchWord_fromNestedDirectory_success() throws IOException {
        var wordToWrite = "Something";
        var filePath = createDummyDirectoryWithFilesContainsWord(wordToWrite);

        indexer.indexDirectory(filePath);
        var paths = indexer.searchWord(wordToWrite);

        Assertions.assertEquals(ConcurrentWordFileIndexerTest.INNER_DIR_COUNT, paths.size());
        var expectedPaths = Arrays
                .stream(filePath.toFile().listFiles())
                .filter(File::isFile)
                .map(File::toPath)
                .collect(Collectors.toSet());
        Assertions.assertEquals(expectedPaths, paths);
    }

    private Path createDummyFileWithWord(String word) throws IOException {
        var path = Paths.get(TEST_DATA_PATH + "/" + UUID.randomUUID());
        return createFileWithWord(path, word);
    }

    private Path createFileWithWord(Path path, String word) throws IOException {
        var file = Paths.get(path.toString() + ".txt").toFile();

        file.createNewFile();

        try (var writer = new FileWriter(file)) {
            writer.write(word);
        }

        return file.toPath();
    }

    private List<Path> createDummyFilesWithWord(String word) {
        return IntStream.range(0, DUMMY_FILES_COUNT).mapToObj(index -> {
            try {
                var path = Paths.get(TEST_DATA_PATH + "/" + index);
                return createFileWithWord(path, word);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).toList();
    }

    private Path createDummyDirectoryWithFilesContainsWord(String word) throws IOException {
        var path = Paths.get(TEST_DATA_PATH + "/nested");

        var dir = path.toFile();
        dir.mkdir();

        for (int i = 0; i < INNER_DIR_COUNT; ++i) {
            var innerDir = Paths.get(path + "/nested" + i).toFile();
            innerDir.mkdir();

            createFileWithWord(innerDir.toPath(), word);
        }

        return path;
    }
}

