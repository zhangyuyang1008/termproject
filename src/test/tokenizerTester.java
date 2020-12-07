package test;

import tokenizer.Tokenizer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class tokenizerTester {
    public static void main(String[] args) throws Exception {
        File file = new File("src/test/test.txt");
        Scanner input = new Scanner(file);
        Tokenizer.runTokenizer(input);
    }
}
