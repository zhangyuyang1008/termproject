package test;

import Binary.Out;
import analyser.Analyser;
import tokenizer.Tokenizer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Scanner;

public class tokenizerTester {
    public static void main(String[] args) throws Exception {
        File file = new File(args[0]);
        Scanner input = new Scanner(file);
        Tokenizer.runTokenizer(input);
        Analyser.analyseProgram();
        Out binary = new Out(Analyser.getGlobalVars(), Analyser.getStartFunction(), Analyser.getFunctionWithInstructionsList());

        DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(args[1])));
        List<Byte> bytes = binary.generate();
        byte[] resultBytes = new byte[bytes.size()];
        for (int i = 0; i < bytes.size(); ++i) {
            resultBytes[i] = bytes.get(i);
        }
        out.write(resultBytes);
    }
}
