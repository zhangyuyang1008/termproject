package Binary;

import analyser.FunctionWithInstructions;
import analyser.GlobalVar;
import instruction.Instruction;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Out {
    private List<GlobalVar> globalVars;
    private FunctionWithInstructions start;
    private List<FunctionWithInstructions> functionInstructions;
    private List<Byte> output;

    int magic=0x72303b3e;
    int version=0x00000001;

    public Out(List<GlobalVar> globals, FunctionWithInstructions start, List<FunctionWithInstructions> functionDefs) throws FileNotFoundException {
        this.globalVars = globals;
        this.start = start;
        this.functionInstructions = functionDefs;
        output = new ArrayList<>();
    }
    public List<Byte> generate() throws IOException {
        //magic
        List<Byte> magic=int2bytes(4,this.magic);
        output.addAll(magic);
        //version
        List<Byte> version=int2bytes(4,this.version);
        output.addAll(version);

        //globals.count
        List<Byte> globalCount=int2bytes(4, globalVars.size());
        output.addAll(globalCount);

        //out.writeBytes(globalCount.toString());

        for(GlobalVar global : globalVars){
            //isConst
            Integer isconst=0;
            if(global.is_const())
                isconst=1;
            List<Byte> isConst=int2bytes(1, isconst);
            output.addAll(isConst);

            //out.writeBytes(isConst.toString());

            // value count
            List<Byte> globalValueCount;
            //out.writeBytes(globalValueCount.toString());

            //value items
            List<Byte> globalValue;
            if (global.getValueItems() == null) {
                globalValueCount = int2bytes(4, 8);
                globalValue = long2bytes(8,0L);
            }
            else {
                globalValue = String2bytes(global.getValueItems());
                globalValueCount = int2bytes(4, globalValue.size());
            }


            output.addAll(globalValueCount);
            output.addAll(globalValue);
            //System.out.println(globalValue.toString());
            //System.out.println("globalValueCount="+globalValueCount);
            //System.out.println(global.getValueItems());
        }

        //functions.count
        List<Byte> functionsCount=int2bytes(4, functionInstructions.size() + 1);
        output.addAll(functionsCount);
        //out.writeBytes(functionsCount.toString());

        generateFunction(start);

        for(FunctionWithInstructions functionDef : functionInstructions){
            generateFunction(functionDef);
        }
        return output;
    }

    private void generateFunction(FunctionWithInstructions functionDef) throws IOException {
        //name
        List<Byte> name = int2bytes(4,functionDef.getId());
        output.addAll(name);
        //out.writeBytes(name.toString());

        //retSlots
        List<Byte> retSlots = int2bytes(4,functionDef.getReturnCount());
        output.addAll(retSlots);
        //out.writeBytes(retSlots.toString());

        //paramsSlots;
        List<Byte> paramsSlots=int2bytes(4,functionDef.getParamCount());
        output.addAll(paramsSlots);
        //out.writeBytes(paramsSlots.toString());

        //locSlots;
        List<Byte> locSlots=int2bytes(4,functionDef.getLocalCount());
        output.addAll(locSlots);
        //out.writeBytes(locSlots.toString());

        List<Instruction> instructions = functionDef.getBody();

        //bodyCount
        List<Byte> bodyCount=int2bytes(4, instructions.size());
        output.addAll(bodyCount);
        //out.writeBytes(bodyCount.toString());

        //instructions
        for(Instruction instruction : instructions){
            //type
            List<Byte> type = int2bytes(1, instruction.getInstruction());
            output.addAll(type);
            //out.writeBytes(type.toString());

            if(instruction.getParam() != null){
                List<Byte>  x;
                if(instruction.getInstruction() == 1)
                    x = long2bytes(8,instruction.getParam());
                else
                    x = int2bytes(4,instruction.getParam());
                output.addAll(x);
                //out.writeBytes(x.toString());
            }
        }
    }

    private List<Byte> Char2bytes(char value) {
        List<Byte>  AB=new ArrayList<>();
        AB.add((byte)(value&0xff));
        return AB;
    }

    private List<Byte> String2bytes(String valueString) {
        List<Byte>  AB=new ArrayList<>();
        for (int i=0;i<valueString.length();i++){
            char ch=valueString.charAt(i);
            AB.add((byte)(ch&0xff));
        }
        return AB;
    }

    private List<Byte> long2bytes(int length, long target) {
        ArrayList<Byte> bytes = new ArrayList<>();
        int start = 8 * (length-1);
        for(int i = 0 ; i < length; i++){
            bytes.add((byte) (( target >> ( start - i * 8 )) & 0xFF ));
        }
        return bytes;
    }

    private ArrayList<Byte> int2bytes(int length,int target){
        ArrayList<Byte> bytes = new ArrayList<>();
        int start = 8 * (length-1);
        for(int i = 0 ; i < length; i++){
            bytes.add((byte) (( target >> ( start - i * 8 )) & 0xFF ));
        }
        return bytes;
    }
}
