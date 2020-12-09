package test;

import Binary.Out;
import analyser.Analyser;
import analyser.FunctionWithInstructions;
import analyser.GlobalVar;
import tokenizer.Tokenizer;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Scanner;

public class tokenizerTester {
    public static void main(String[] args) throws Exception {
        try{
            File file = new File(args[0]);
            Scanner input = new Scanner(file);
            Tokenizer.runTokenizer(input);
            System.out.println("\n------------------Analyser Start");
            Analyser.analyseProgram();
            System.out.println(Analyser.getGlobalVars().size());
            for (GlobalVar global : Analyser.getGlobalVars()) {
                System.out.println(global);
            }
            System.out.println("-----------------------------function");
            System.out.println(Analyser.getStartFunction());
            for (FunctionWithInstructions functionDef : Analyser.getFunctionWithInstructionsList()) {
                System.out.println(functionDef);
            }
            System.out.println("\n----------------------------生成二进制");
            Out binary = new Out(Analyser.getGlobalVars(), Analyser.getStartFunction(), Analyser.getFunctionWithInstructionsList());

            DataOutputStream out = new DataOutputStream(new FileOutputStream(new File(args[1])));
            List<Byte> bytes = binary.generate();
            byte[] resultBytes = new byte[bytes.size()];
            for (int i = 0; i < bytes.size(); ++i) {
                resultBytes[i] = bytes.get(i);
            }
            out.write(resultBytes);
        }catch (Exception e) {
            System.exit(-1);
        }
    }
}
