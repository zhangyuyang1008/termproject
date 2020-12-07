package tokenizer;

public enum TokenType {
    //关键字
    /** Fn */
    FN_KW,
    /** Let */
    LET_KW,
    /** Const */
    CONST_KW,
    /** As */
    AS_KW,
    /** While */
    WHILE_KW,
    /** If */
    IF_KW,
    /** Else */
    ELSE_KW,
    /** Return */
    RETURN_KW,
    /** Break */
    BREAK_KW,
    /** Continue */
    CONTINUE_KW,

    //字面量
    /** 无符号整数 */
    UINT_LITERAL,
    /** 字符串常量 */
    STRING_LITERAL,
    /** 浮点数 */
    DOUBLE_LITERAL,
    /** 字符常量 */
    CHAR_LITERAL,

    //标识符
    /** 标识符 */
    IDENT,

    //符号
    /** 加号 */
    PLUS,
    /** 减号 */
    MINUS,
    /** 乘号 */
    MUL,
    /** 除号 */
    DIV,
    /** 赋值 */
    ASSIGN,
    /** 等于 */
    EQ,
    /** 不等于 */
    NEQ,
    /** 小于 */
    LT,
    /** 大于 */
    GT,
    /** 小于等于 */
    LE,
    /** 大于等于 */
    GE,
    /** 左括号 */
    L_PAREN,
    /** 右括号 */
    R_PAREN,
    /** 左大括号 */
    L_BRACE,
    /** 右大括号 */
    R_BRACE,
    /** 箭头 */
    ARROW,
    /** 逗号 */
    COMMA,
    /** 冒号 */
    COLON,
    /** 分号 */
    SEMICOLON,

    //注释
    /** 注释 */
    COMMENT;

    @Override
    public String toString() {
        switch (this) {
            //关键字
            case FN_KW:
                return "FN_KW";
            case LET_KW:
                return "LET_KW";
            case CONST_KW:
                return "CONST_KW";
            case AS_KW:
                return "AS_KW";
            case WHILE_KW:
                return "WHILE_KW";
            case IF_KW:
                return "IF_KW";
            case ELSE_KW:
                return "ELSE_KW";
            case RETURN_KW:
                return "RETURN_KW";
            case BREAK_KW:
                return "BREAK_KW";
            case CONTINUE_KW:
                return "CONTINUE_KW";

            //字面量
            case UINT_LITERAL:
                return "UINT_LITERAL";
            case STRING_LITERAL:
                return "STRING_LITERAL";
            case DOUBLE_LITERAL:
                return "DOUBLE_LITERAL";
            case CHAR_LITERAL:
                return "CHAR_LITERAL";

            //标识符
            case IDENT:
                return "IDENT";

            //运算符
            case PLUS:
                return "PLUS";
            case MINUS:
                return "MINUS";
            case MUL:
                return "MUL";
            case DIV:
                return "DIV";
            case ASSIGN:
                return "ASSIGN";
            case EQ:
                return "EQ";
            case NEQ:
                return "NEQ";
            case LT:
                return "LT";
            case GT:
                return "GT";
            case LE:
                return "LE";
            case GE:
                return "GE";
            case L_PAREN:
                return "L_PAREN";
            case R_PAREN:
                return "R_PAREN";
            case L_BRACE:
                return "L_BRACE";
            case R_BRACE:
                return "R_BRACE";
            case ARROW:
                return "ARROW";
            case COMMA:
                return "COMMA";
            case COLON:
                return "COLON";
            case SEMICOLON:
                return "SEMICOLON";
            case COMMENT:
                return "COMMENT";

            default:
                return "InvalidToken";
        }
    }
}
