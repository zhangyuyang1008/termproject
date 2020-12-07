package instruction;

public class Instruction {
    private InstructionType type;
    //为了保证能填入null，不使用int
    private Integer param;
    public Instruction(InstructionType type, Integer param) {
        this.type= type;
        this.param = param;
    }
}
