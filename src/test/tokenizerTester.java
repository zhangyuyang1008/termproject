package test;

import Binary.Out;
import analyser.Analyser;
import analyser.FunctionWithInstructions;
import analyser.GlobalVar;
import tokenizer.Tokenizer;

import java.io.*;
import java.util.List;
import java.util.Scanner;

public class tokenizerTester {
    public static void main(String[] args) throws Exception {
        File ctoFile = new File(args[0]);

        InputStreamReader rdCto = new InputStreamReader(new FileInputStream(ctoFile));

        BufferedReader bfReader = new BufferedReader(rdCto);

        String txtline = null;

        while ((txtline = bfReader.readLine()) != null) {

            System.out.println(txtline);

        }
        bfReader.close();
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
