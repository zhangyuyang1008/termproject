package analyser;

import instruction.Instruction;

public class Break {
    private Instruction ainstruction;
    private int pos;
    private int whileLevel;

    public Break(Instruction ainstruction, int pos, int whileLevel){
        this.ainstruction=ainstruction;
        this.pos=pos;
        this.whileLevel=whileLevel;
    }
    public void setAinstruction(Instruction ainstruction) {
        this.ainstruction = ainstruction;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public void setWhileLevel(int whileLevel) {
        this.whileLevel = whileLevel;
    }

    public Instruction getAinstruction() {
        return ainstruction;
    }

    public int getPos() {
        return pos;
    }

    public int getWhileLevel() {
        return whileLevel;
    }
}
