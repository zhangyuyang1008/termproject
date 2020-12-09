package instruction;

public class Instruction {
    private InstructionType type;
    //为了保证能填入null，不使用int
    private Integer param;
    public Instruction(InstructionType type, Integer param) {
        this.type= type;
        this.param = param;
    }

    public InstructionType getType() {
        return type;
    }

    public Integer getParam() {
        return param;
    }

    public void setType(InstructionType type) {
        this.type = type;
    }

    public void setParam(Integer param) {
        this.param = param;
    }
    public Integer getInstruction() {
        return this.getType().getValue();
    }

    @Override
    public String toString() {
        return "\n      Instructions{\n" +
                "           instruction=" + this.getType() +
                ",\n        paramIds=" + param + '\n'+
                '}';
    }
}
