package instruction;

public enum  InstructionType{
    nop,
    push,
    pop,
    popn,
    dup,
    loca,
    arga,
    globa,
    load,
    store,
    alloc,
    free,
    stackalloc,
    add,
    sub,
    mul,
    div,
//    shl,
//    shr,
//    and,
//    or,
    cmp,
    neg,
    setLt,
    setGt,
    not,
//    itof,
//    shrl,
    br,
    brFrue,
    brFalse,
    call,
    ret,
    callname;
//    scan,
//    print,
//    println,
//    panic;


    public Integer getValue() {
        switch (this) {
            case nop:
                return 0x00;
            case push:
                return 0x01;
            case pop:
                return 0x02;
            case popn:
                return 0x03;
            case dup:
                return 0x04;
            case loca:
                return 0x0a;
            case arga:
                return 0x0b;
            case globa:
                return 0x0c;
            case load:
                return 0x13;
            case store:
                return 0x17;
            case alloc:
                return 0x18;
            case free:
                return 0x19;
            case stackalloc:
                return 0x1a;
            case add:
                return 0x20;
            case sub:
                return 0x21;
            case mul:
                return 0x22;
            case div:
                return 0x23;
            case cmp:
                return 0x30;
            case neg:
                return 0x34;
            case setLt:
                return 0x39;
            case setGt:
                return 0x3a;
            case not:
                return 0x2e;
            case call:
                return 0x48;
            case ret:
                return 0x49;
            case callname:
                return 0x4a;
            case br:
                return 0x41;
            case brFalse:
                return 0x42;
            case brFrue:
                return 0x43;
            default:
                return 0xfff;

        }
    }
}
