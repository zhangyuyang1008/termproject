package analyser;


import error.*;
import instruction.Instruction;
import instruction.InstructionType;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;
import util.Pos;

import java.text.Format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public final class Analyser {
    //词法分析
    private static Tokenizer tokenizer;
    //当前token
    private static Token tokenNow;
    //指令集
    private static List<Instruction> instructions;
    //变量表
    private static List<Var> Vars = new ArrayList<>();
    //常量表
    private static List<Const> Consts = new ArrayList<>();
    //函数表
    private static List<Function> Functions = new ArrayList<>();
    //全局变量表
    private static List<GlobalVar> globalVars = new ArrayList<>();
    //函数参数列表
    private static List<Param> params;
    //函数返回值列表
    //局部变量个数
    private static int localVarCount = 0;
    //全局变量个数
    private static int globalVarCount = 0;
    //函数个数
    private static int functionCount = 0;
    //操作符栈
    private static Stack<TokenType> operatorStack = new Stack<>();
    //是否在赋值
    private static boolean Assign;
    //是否有返回值
    private static boolean hasReturn;
    //起始地址
    private static int address;
    //带指令函数列表
    private static List<FunctionWithInstructions> FunctionWithInstructionsList = new ArrayList<>();




    public static void analyseProgram() throws Exception{
        //读取要分析的token
        tokenNow = Tokenizer.getToken();
        //初始化指令集
        instructions = new ArrayList<>();
        //program -> decl_stmt* function*
        //decl_stmt*
        //decl_stmt -> let_decl_stmt | const_decl_stmt
        while (tokenNow.getTokenType()==TokenType.CONST_KW||tokenNow.getTokenType()==TokenType.LET_KW){
            //第一层的声明语句
            analyseDeclStmt(1);
            //读下一个token
            tokenNow = Tokenizer.getToken();
        }
        //此时获取了程序初始化用到的指令
        List<Instruction> programInitInstruction = instructions;
        //function*
        while (tokenNow != null) {
            if (tokenNow.getTokenType() != TokenType.FN_KW)
                throw new AnalyzeError(ErrorCode.ExpectedToken,tokenNow.getStartPos());
            //指令集
            instructions = new ArrayList<>();
            //当前参数
            params = new ArrayList<>();
            //初始化
            localVarCount = 0;
            hasReturn = false;

            //分析函数
            analyseFunction();

            //读下一个token
            tokenNow = Tokenizer.getToken();
        }


    }
    //let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
    //const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
    public static void analyseDeclStmt(int level) throws Exception {
        if (tokenNow.getTokenType() == TokenType.CONST_KW)
            analyseConstDeclStmt(level);
        else if (tokenNow.getTokenType() == TokenType.LET_KW)
            analyseLetDeclStmt(level);
        else throw new AnalyzeError(ErrorCode.ExpectedToken,tokenNow.getStartPos());
        //全局
        if (level == 1) globalVarCount++;
        //局部
        else localVarCount++;
    }

    //const_decl_stmt -> 'const' IDENT ':' ty '=' expr ';'
    public static void analyseConstDeclStmt(int level) throws Exception {
        if (tokenNow.getTokenType() != TokenType.CONST_KW)
            throw new AnalyzeError(ErrorCode.noConstant,tokenNow.getStartPos());

        //读下一个token，应该为IDENT
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.IDENT)
            throw new AnalyzeError(ErrorCode.NeedIdentifier,tokenNow.getStartPos());
        //获取IDENT的名称
        String name = (String) tokenNow.getValue();
        //查表，看是否重复定义
        if (!isNewName(name, level))
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration,tokenNow.getStartPos());
        //当是在第一层即最外层定义，就填入全局变量表
        if (level == 1) {
            //创建全局变量并填表
            GlobalVar global = new GlobalVar(true);
            globalVars.add(global);
            //同时是常量，填入常量表
            Const constant = new Const(name, globalVarCount, level);
            Consts.add(constant);
            //生成globa指令
            Instruction instruction = new Instruction(InstructionType.globa, globalVarCount);
            instructions.add(instruction);
        }
        //在其他层则是局部常量
        else {
            Const constant = new Const(name, localVarCount, level);
            Consts.add(constant);

            //生成loca指令
            Instruction instruction = new Instruction(InstructionType.loca, localVarCount);
            instructions.add(instruction);
        }

        //读下一个token，应该为':'
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.COLON)
            throw new AnalyzeError(ErrorCode.noColon,tokenNow.getStartPos());

        //读下一个token，应该为'ty'
        tokenNow = Tokenizer.getToken();
        String type = analyseTy();
        //此处不是函数返回值，只能是int
        if (!type.equals("int"))
            throw new AnalyzeError(ErrorCode.noInt,tokenNow.getStartPos());

        //读下一个token，应该是ASSIGN
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.ASSIGN)
            throw new AnalyzeError(ErrorCode.ConstantNeedValue,tokenNow.getStartPos());

        Assign = true;
        //读下一个token，应该去分析expr
        tokenNow = Tokenizer.getToken();
        analyseExpr(level);

        //清空操作符栈（对应opg算法中读到'#'的操作）,并将与符号对应的指令填入指令集
        while (!operatorStack.empty()) {
            TokenType tokenType = operatorStack.pop();
            operatorGetInstruction(tokenType, instructions);
        }
        Assign = false;

        //读下一个token，应该是SEMICOLON
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.SEMICOLON)
            throw new AnalyzeError(ErrorCode.NoSemicolon,tokenNow.getStartPos());

        //储存
        Instruction instruction = new Instruction(InstructionType.store, null);
        instructions.add(instruction);
    }

    //let_decl_stmt -> 'let' IDENT ':' ty ('=' expr)? ';'
    public static void analyseLetDeclStmt(Integer level) throws Exception {
        if (tokenNow.getTokenType() != TokenType.LET_KW)
            throw new AnalyzeError(ErrorCode.noLet,tokenNow.getStartPos());

        //读下一个token，应该是IDENT
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.IDENT)
            throw new AnalyzeError(ErrorCode.NeedIdentifier,tokenNow.getStartPos());
        //获取变量名字
        String name = (String) tokenNow.getValue();
        //查表看是否重复定义
        if (!isNewName(name, level))
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration,tokenNow.getStartPos());
        //是全局变量
        if (level == 1) {
            Var variable = new Var(name, globalVarCount, level);
            Vars.add(variable);
            GlobalVar global = new GlobalVar(false);
            globalVars.add(global);
        }
        //是局部变量
        else {
            Var variable = new Var(name, localVarCount, level);
            Vars.add(variable);
        }

        //读下一个token，应该是COLON
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.COLON)
            throw new AnalyzeError(ErrorCode.noColon,tokenNow.getStartPos());

        //读下一个token，应该为'ty'
        tokenNow = Tokenizer.getToken();
        String type = analyseTy();
        if (!type.equals("int"))
            throw new AnalyzeError(ErrorCode.noInt,tokenNow.getStartPos());

        //读下一个token，若是ASSIGN应该继续往后分析表达式
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.ASSIGN) {
            Assign = true;
            Instruction instruction;
            //是全局变量
            if (level == 1) {
                instruction = new Instruction(InstructionType.globa, globalVarCount);
            }
            //是局部变量
            else {
                instruction = new Instruction(InstructionType.loca, localVarCount);
            }
            instructions.add(instruction);

            //读下一个token，分析表达式expr
            tokenNow = Tokenizer.getToken();
            analyseExpr(level);
            //清空操作符栈（对应opg算法中读到'#'的操作）,并将与符号对应的指令填入指令集
            while (!operatorStack.empty()) {
                TokenType tokenType = operatorStack.pop();
                operatorGetInstruction(tokenType, instructions);
            }

            //储存
            instruction = new Instruction(InstructionType.store, null);
            instructions.add(instruction);

            Assign = false;
        }

        //读下一个token，应该是SEMICOLON
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.SEMICOLON)
            throw new AnalyzeError(ErrorCode.NoSemicolon,tokenNow.getStartPos());
    }

    //function -> 'fn' IDENT '(' function_param_list? ')' '->' ty block_stmt
    public static void analyseFunction() throws Exception {
        //读下一个token，应该是IDENT（函数名）
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.IDENT)
            throw new AnalyzeError(ErrorCode.ExpectedToken,tokenNow.getStartPos());
        //查函数表
        String name = (String) tokenNow.getValue();
        if (!isNewFunction(name))
            throw new AnalyzeError(ErrorCode.DuplicateDeclaration,tokenNow.getStartPos());

        //读下一个token，应该是左括号
        tokenNow = Tokenizer.getToken();
        if (!(tokenNow.getTokenType() == TokenType.L_PAREN))
            throw new AnalyzeError(ErrorCode.noLP,tokenNow.getStartPos());
        //参数列表
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.R_PAREN)
            analyseFunctionParamList();

        //右括号
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.R_PAREN)
            throw new AnalyzeError(ErrorCode.noRP,tokenNow.getStartPos());
        //箭头
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.ARROW)
            throw new AnalyzeError(ErrorCode.noArrow,tokenNow.getStartPos());
        //返回值类型
        tokenNow = Tokenizer.getToken();
        String type = analyseTy();
        int returnCount;
        if (type.equals("int")) {
            returnCount = 1;
            address = 1;
        }
        else {
            returnCount = 0;
            address = 0;
            hasReturn = true;
        }


        Function function = new Function(type, name, params, functionCount);
        Functions.add(function);

        //读下一个token，应该分析block
        tokenNow = Tokenizer.getToken();
        //分析代码块
        analyseBlockStmt(type, 2);

        //若没有返回
        if (!hasReturn)
            throw new AnalyzeError(ErrorCode.noReturn,tokenNow.getStartPos());

        if (type.equals("void")) {
            Instruction instruction = new Instruction(InstructionType.ret, null);
            instructions.add(instruction);
        }

        //添加函数名为全局变量
        functionIntoGlobals(name);

        FunctionWithInstructions FunctionWithInstructions = new FunctionWithInstructions(globalVarCount, returnCount, params.size(), localVarCount, instructions);
        FunctionWithInstructionsList.add(FunctionWithInstructions);

        //清除局部变量
        clearLocal();

        //全局变量个数加一
        globalVarCount++;
        //函数个数加一
        functionCount++;
    }

    //ty -> IDENT
    public static String analyseTy() throws Exception {
        //应该为IDENT
        if (tokenNow.getTokenType() != TokenType.IDENT)
            throw new AnalyzeError(ErrorCode.ExpectedToken,tokenNow.getStartPos());

        //获取类型
        String type = (String) tokenNow.getValue();

        //基础c0是int或void(另外再提醒一下，返回值类型 return_type 即使为 void 也不能省略)
        if (!(type.equals("int") || type.equals("void")))
            throw new AnalyzeError(ErrorCode.InvalidIdentifier,tokenNow.getStartPos());

        return type;
    }

    //表达式：expr ->
    //      operator_expr
    //    | negate_expr
    //    | assign_expr
    //    | as_expr
    //    | call_expr
    //    | literal_expr
    //    | ident_expr
    //    | group_expr
    public static void analyseExpr(Integer level) throws Exception {
        if (symbol.getType() == TokenType.MINUS) {
            Instructions negInstruction = new Instructions(Instruction.neg, null);
            symbol = Tokenizer.readToken();
            if (symbol.getType() == TokenType.MINUS) {
                analyseExpr(level);
                instructionsList.add(negInstruction);
            }
            else if (symbol.getType() == TokenType.UINT_LITERAL) {
                analyseLiteralExpr();
                instructionsList.add(negInstruction);
                if (Format.isOperator(symbol)) {
                    analyseOperatorExpr(level);
                }
            }else if (symbol.getType() == TokenType.IDENT) {
                String name = (String) symbol.getVal();
                symbol = Tokenizer.readToken();
                if (symbol.getType() == TokenType.L_PAREN) {
                    stackOp.push(TokenType.L_PAREN);
                    if (Format.isFunction(name, Functions)) {
                        Integer id;
                        Instructions instruction;
                        // 是库函数
                        if (Format.isStaticFunction(name)) {
                            LibraryFunction function = new LibraryFunction(name, globalCount);
                            libraryFunctions.add(function);
                            id = globalCount;
                            globalCount++;

                            Global global = Format.functionNameToGlobalInformation(name);
                            globals.add(global);
                            instruction = new Instructions(Instruction.callname, id);
                        }
                        //自定义函数
                        else {
                            id = Format.getFunctionId(name, Functions);
                            instruction = new Instructions(Instruction.call, id);
                        }
                        analyseCallExpr(name, level);

                        //弹栈
                        while (stackOp.peek() != TokenType.L_PAREN) {
                            TokenType tokenType = stackOp.pop();
                            Format.instructionGenerate(tokenType, instructionsList);
                        }
                        stackOp.pop();

                        instructionsList.add(instruction);
                    }else {
                        throw new AnalyzeError(ErrorCode.InValidFunction);
                    }
                    instructionsList.add(negInstruction);
                    if (Format.isOperator(symbol)) {
                        analyseOperatorExpr(level);
                    }
                }else if (Format.isOperator(symbol)) {
                    analyseIdentExpr(name, level);
                    instructionsList.add(negInstruction);
                    analyseOperatorExpr(level);
                }
                else {
                    analyseIdentExpr(name, level);
                    instructionsList.add(negInstruction);
                }
            }
        }
        else if (symbol.getType() == TokenType.IDENT) {
            String name = (String) symbol.getVal();
            symbol = Tokenizer.readToken();
            if (symbol.getType() == TokenType.ASSIGN) {
                if (onAssign)
                    throw new AnalyzeError(ErrorCode.InvalidAssignment);

                if ((!Format.isConstant(name, Constants) && Format.isVariable(name, Variables)) || Format.isParam(name, params)) {
                    if (Format.isLocal(name, Constants, Variables)) {
                        Integer id = Format.getId(name, level, Constants, Variables);
                        //取出值
                        Instructions instruction = new Instructions(Instruction.loca, id);
                        instructionsList.add(instruction);
                    }else if (Format.isParam(name, params)) {
                        Integer id = Format.getParamPos(name, params);
                        //取出值
                        Instructions instruction = new Instructions(Instruction.arga, alloc + id);
                        instructionsList.add(instruction);
                    }
                    else {
                        Integer id = Format.getId(name, level, Constants, Variables);
                        Instructions instruction = new Instructions(Instruction.globa, id);
                        instructionsList.add(instruction);
                    }
                    onAssign = true;
                    analyseAssignExpr(name, level);
                }else {
                    throw new AnalyzeError(ErrorCode.InvalidAssignment);
                }
                onAssign = false;
                if (Format.isOperator(symbol)) {
                    analyseOperatorExpr(level);
                }
            }
            else if (symbol.getType() == TokenType.L_PAREN) {
                stackOp.push(TokenType.L_PAREN);
                if (Format.isFunction(name, Functions)) {
                    Integer id;
                    Instructions instruction;
                    // 是库函数
                    if (Format.isStaticFunction(name)) {
                        LibraryFunction function = new LibraryFunction(name, globalCount);
                        libraryFunctions.add(function);
                        id = globalCount;
                        globalCount++;

                        Global global = Format.functionNameToGlobalInformation(name);
                        globals.add(global);
                        instruction = new Instructions(Instruction.callname, id);
                    }
                    //自定义函数
                    else {
                        id = Format.getFunctionId(name, Functions);
                        instruction = new Instructions(Instruction.call, id);
                    }
                    analyseCallExpr(name, level);

                    //弹栈
                    while (stackOp.peek() != TokenType.L_PAREN) {
                        TokenType tokenType = stackOp.pop();
                        Format.instructionGenerate(tokenType, instructionsList);
                    }
                    stackOp.pop();

                    instructionsList.add(instruction);

                }else {
                    throw new AnalyzeError(ErrorCode.InValidFunction);
                }
                if (Format.isOperator(symbol)) {
                    analyseOperatorExpr(level);
                }
            }else if (Format.isOperator(symbol)) {
                analyseIdentExpr(name, level);
                analyseOperatorExpr(level);
            }else if (symbol.getType() == TokenType.AS_KW) {
                analyseAsExpr();
            }
            else {
                analyseIdentExpr(name, level);
            }
        }
        else if (symbol.getType() == TokenType.UINT_LITERAL ||
                symbol.getType() == TokenType.STRING_LITERAL) {
            analyseLiteralExpr();
            if (Format.isOperator(symbol)) analyseOperatorExpr(level);
        }
        else if (symbol.getType() == TokenType.L_PAREN) {
            stackOp.push(TokenType.L_PAREN);
            analyseGroupExpr(level);
            if (Format.isOperator(symbol)) {
                analyseOperatorExpr(level);
            }
        }
        else throw new AnalyzeError(ErrorCode.InvalidType);
    }

    //block_stmt -> '{' stmt* '}'
    public static void analyseBlockStmt(String type, Integer level) throws Exception {
        if (tokenNow.getTokenType() != TokenType.L_BRACE)
            throw new AnalyzeError(ErrorCode.noLB,tokenNow.getStartPos());

        //读下一个token
        tokenNow = Tokenizer.getToken();
        while (tokenNow.getTokenType() != TokenType.R_BRACE) {
            //分析语句：Stmt
            analyseStmt(type, level);
        }
    }

    //语句
    public static void analyseStmt(String type, Integer level) throws Exception {
        if (tokenNow.getTokenType() == TokenType.CONST_KW || tokenNow.getTokenType() == TokenType.LET_KW)
            analyseDeclStmt(level);
        else if (tokenNow.getTokenType() == TokenType.IF_KW)
            analyseIfStmt(type, level);
        else if (tokenNow.getTokenType() == TokenType.WHILE_KW)
            analyseWhileStmt(type, level);
        else if (tokenNow.getTokenType() == TokenType.RETURN_KW)
            analyseReturnStmt(type, level);
        else if (tokenNow.getTokenType() == TokenType.SEMICOLON)
            analyseEmptyStmt();
        else if (tokenNow.getTokenType() == TokenType.L_BRACE)
            analyseBlockStmt(type, level + 1);
        else
            analyseExprStmt(level);
    }



    //非分析产生式的部分

    //查看是否重复定义
    public static boolean isNewName(String name, Integer level) {
        //查看是否是8个可以直接调用的函数之一
        if (isStaticFunction(name)) return false;
        //查看是否已经在函数表中
        for (Function function : Functions) {
            if (function.getFunction_name().equals(name) && level == 1)
                return false;
        }
        //查看是否已经在常量表中
        for (Const constant : Consts) {
            if (constant.getName().equals(name) && level == constant.getLevel())
                return false;
        }
        //查看是否已经在变量表中
        for (Var variable : Vars) {
            if (variable.getName().equals(name) && level == variable.getLevel())
                return false;
        }
        //都不在，为新声明，没有重复定义
        return true;
    }

    //是否是8个可以直接调用的函数之一
    public static boolean isStaticFunction(String name){
        if(name.equals("getint")
                || name.equals("getdouble")
                || name.equals("getchar")
                || name.equals("putint")
                || name.equals("putdouble")
                || name.equals("putchar")
                || name.equals("putstr")
                || name.equals("putln"))
            return true;
        return false;
    }

    //从操作符对应指令，并填入指令集
    public static void operatorGetInstruction(TokenType type, List<Instruction> instructionsList) {
        Instruction instruction;
        switch (type) {
            case MINUS:
                instruction = new Instruction(InstructionType.sub, null);
                instructionsList.add(instruction);
                break;
            case MUL:
                instruction = new Instruction(InstructionType.mul, null);
                instructionsList.add(instruction);
                break;
            case DIV:
                instruction = new Instruction(InstructionType.div, null);
                instructionsList.add(instruction);
                break;
            case EQ:
                instruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(instruction);
                instruction = new Instruction(InstructionType.not, null);
                instructionsList.add(instruction);
                break;
            case NEQ:
                instruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(instruction);
                break;
            case LT:
                instruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(instruction);
                instruction = new Instruction(InstructionType.setLt, null);
                instructionsList.add(instruction);
                break;
            case GT:
                instruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(instruction);
                instruction = new Instruction(InstructionType.setGt, null);
                instructionsList.add(instruction);
                break;
            case LE:
                instruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(instruction);
                instruction = new Instruction(InstructionType.setGt, null);
                instructionsList.add(instruction);
                instruction = new Instruction(InstructionType.not, null);
                instructionsList.add(instruction);
                break;
            case GE:
                instruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(instruction);
                instruction = new Instruction(InstructionType.setLt, null);
                instructionsList.add(instruction);
                instruction = new Instruction(InstructionType.not, null);
                instructionsList.add(instruction);
                break;
            case PLUS:
                instruction = new Instruction(InstructionType.add, null);
                instructionsList.add(instruction);
                break;
            default:
                break;
        }

    }

    //查函数表
    public static boolean isNewFunction(String name) {
        if (isStaticFunction(name)) return false;
        for (Function function : Functions) {
            if (function.getFunction_name().equals(name)) return false;
        }
        return true;
    }

    //添加函数为全局变量
    public static void functionIntoGlobals(String name) {
        char[] arr = name.toCharArray();
        int len = arr.length;
        String items = "";
        for (int i = 0; i < len; i++) {
            int asc = (int) arr[i];
            items = items + String.format("%2X", asc);
        }
        GlobalVar global = new GlobalVar(true, arr.length, name);
        globalVars.add(global);
    }

    //清除局部变量表
    public static void clearLocal() {
        int len = Vars.size();
        for (int i = len - 1; i >= 0; --i) {
            Var variable = Vars.get(i);
            if (variable.getLevel() > 1)
                Vars.remove(i);
        }
        len = Consts.size();
        for (int i = len - 1; i >= 0; --i) {
            Const constant = Consts.get(i);
            if (constant.getLevel() > 1)
                Consts.remove(i);
        }
    }

}
