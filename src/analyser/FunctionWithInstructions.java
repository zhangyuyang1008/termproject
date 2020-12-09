package analyser;

import instruction.Instruction;

import java.util.List;

//函数输出指令
public class FunctionWithInstructions {
    private Integer id;
    private Integer returnCount;
    private Integer paramCount;
    private Integer localCount;
    private List<Instruction> body;

    public FunctionWithInstructions(Integer name, Integer returnCount, Integer paramCount, Integer localCount, List<Instruction> body) {
        this.id = name;
        this.returnCount = returnCount;
        this.paramCount = paramCount;
        this.localCount = localCount;
        this.body = body;
    }

    public Integer getId() {
        return id;
    }

    public Integer getReturnCount() {
        return returnCount;
    }

    public Integer getParamCount() {
        return paramCount;
    }

    public Integer getLocalCount() {
        return localCount;
    }

    public List<Instruction> getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "FunctionDef{\n" +
                "   id=" + id +
                ",\n    returnSlots=" + returnCount +
                ",\n    paramSlots=" + paramCount +
                ",\n    localSlots=" + localCount +
                ",\n    body=" + body +'\n'+
                '}';
    }
}
