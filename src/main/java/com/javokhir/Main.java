package com.javokhir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;

public class Main {
    private final ConcurrentWordFileIndexer indexer = new ConcurrentWordFileIndexer(true);

    public static void main(String[] args) {
        new Main().consoleInterface();
    }

    private void consoleInterface() {
        Scanner scanner = new Scanner(System.in);

        do {
            System.out.println("Menu:");
            System.out.println("D - Index a directory");
            System.out.println("F - Index a file");
            System.out.println("S - Search for a word");
            System.out.println("Q - Quit");

            System.out.print("Select an option: ");
            String selectedOption = scanner.nextLine();

            switch (selectedOption) {
                case "D": {
                    indexDirectory(scanner);
                    break;
                }
                case "F": {
                    indexFile(scanner);
                    break;
                }
                case "S": {
                    searchWord(scanner);
                    break;
                }
                case "Q": {
                    System.out.println("Exiting...");
                    System.exit(0);
                    break;
                }
                default: {
                    System.out.println("Invalid option, please try again.");
                }
            }
        } while (true);
    }

    private void indexDirectory(Scanner scanner) {
        System.out.print("Enter the directory path to index: ");
        String path = scanner.nextLine();
        System.out.println("Indexing...");
        indexer.indexDirectory(Paths.get(path));
        System.out.println("Indexing finished.");
    }

    private void indexFile(Scanner scanner) {
        System.out.print("Enter the file path to index: ");
        String path = scanner.nextLine();
        System.out.println("Indexing...");
        indexer.indexFile(Paths.get(path));
        System.out.println("Indexing finished.");
    }

    private void searchWord(Scanner scanner) {
        System.out.print("Enter the word to search: ");
        String word = scanner.nextLine();
        System.out.println("Searching...");
        Set<Path> files = indexer.searchWord(word);

        if(files.isEmpty()) {
            System.out.println("No files found containing the word: " + word);
        } else {
            System.out.println("Files containing the word: ");
            for(Path file : files) {
                System.out.println(file);
            }
        }
    }
}