package analyser;


import error.*;
import instruction.Instruction;
import instruction.InstructionType;
import tokenizer.Token;
import tokenizer.TokenType;
import tokenizer.Tokenizer;

import java.util.ArrayList;
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
    //start函数
    private static FunctionWithInstructions startFunction;
    //局部变量个数
    private static int localVarCount = 0;
    //全局变量个数
    private static int globalVarCount = 0;
    //函数个数
    private static int functionCount = 1;
    //操作符栈
    private static Stack<TokenType> operatorStack = new Stack<>();
    //是否在赋值
    private static boolean Assign;
    //是否在循环
    private static boolean Circle;
    //是否有返回值
    private static boolean hasReturn;
    //起始地址
    private static int address = 0;
    //带指令函数列表
    private static List<FunctionWithInstructions> FunctionWithInstructionsList = new ArrayList<>();
    //算符优先矩阵
    private static int priority[][]={
            {1,1,-1,-1,-1,1,   1,1,1,1,1,1,  -1},
            {1,1,-1,-1,-1,1,  1,1,1,1,1,1,   -1},
            {1,1,1,1,-1,1,   1,1,1,1,1,1,    -1},
            {1,1,1,1,-1,1,   1,1,1,1,1,1,   -1},

            {-1,-1,-1,-1,-1,100,   -1,-1,-1,-1,-1,-1,   -1},
            {-1,-1,-1,-1,0,0   ,    -1,-1,-1,-1,-1,-1   ,-1},

            {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
            {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
            {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
            {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
            {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},
            {-1,-1,-1,-1,-1,1,  1,1,1,1,1,1,      -1},

            {1,1,1,1,-1,1,     1,1,1,1,1,1    ,-1}

    };



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

        }

        //判断是否有main函数
        if(!hasMain())
            throw new AnalyzeError(ErrorCode.noMain,tokenNow.getStartPos());

        //向全局变量填入口程序_start
        GlobalVar global = new GlobalVar(true, 6, "_start");
        globalVars.add(global);
        //start函数没有参数
        Instruction ainstruction = new Instruction(InstructionType.stackalloc, 0);
        programInitInstruction.add(ainstruction);
        if (mainHasReturn()) {
            //add call main
            ainstruction.setParam(1);
            //main函数最后声明
            System.out.println("functionCount"+functionCount);
            ainstruction = new Instruction(InstructionType.call, functionCount-1);
            programInitInstruction.add(ainstruction);
            ainstruction = new Instruction(InstructionType.popn, 1);
            programInitInstruction.add(ainstruction);
        }else {
            //add call main
            System.out.println("functionCount"+functionCount);
            ainstruction = new Instruction(InstructionType.call, functionCount-1);
            programInitInstruction.add(ainstruction);
        }
        startFunction = new FunctionWithInstructions(globalVarCount, 0, 0, 0, programInitInstruction);
        globalVarCount++;



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
            Instruction ainstruction = new Instruction(InstructionType.globa, globalVarCount);
            instructions.add(ainstruction);
        }
        //在其他层则是局部常量
        else {
            Const constant = new Const(name, localVarCount, level);
            Consts.add(constant);

            //生成loca指令
            Instruction ainstruction = new Instruction(InstructionType.loca, localVarCount);
            instructions.add(ainstruction);
        }

        //读下一个token，应该为':'
        tokenNow = Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.COLON)
            throw new AnalyzeError(ErrorCode.noColon,tokenNow.getStartPos());

        //读下一个token，应该为'ty'
        tokenNow = Tokenizer.getToken();
        String type = analyseTy();
        //此处不是函数返回值，只能是int
        if (!(type.equals("int")||type.equals("char")))
            throw new AnalyzeError(ErrorCode.noInt,tokenNow.getStartPos());

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


        if (tokenNow.getTokenType() != TokenType.SEMICOLON)
            throw new AnalyzeError(ErrorCode.NoSemicolon,tokenNow.getStartPos());

        //储存
        Instruction ainstruction = new Instruction(InstructionType.store, null);
        instructions.add(ainstruction);//读下一个token，应该是SEMICOLON
        tokenNow = Tokenizer.getToken();
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
        if (!(type.equals("int")||type.equals("char")))
            throw new AnalyzeError(ErrorCode.noInt,tokenNow.getStartPos());


        if (tokenNow.getTokenType() == TokenType.ASSIGN) {
            Assign = true;
            Instruction ainstruction;
            //是全局变量
            if (level == 1) {
                ainstruction = new Instruction(InstructionType.globa, globalVarCount);
            }
            //是局部变量
            else {
                ainstruction = new Instruction(InstructionType.loca, localVarCount);
            }
            instructions.add(ainstruction);

            //读下一个token，分析表达式expr
            tokenNow = Tokenizer.getToken();
            analyseExpr(level);
            //清空操作符栈（对应opg算法中读到'#'的操作）,并将与符号对应的指令填入指令集
            while (!operatorStack.empty()) {
                TokenType tokenType = operatorStack.pop();
                operatorGetInstruction(tokenType, instructions);
            }

            //储存
            ainstruction = new Instruction(InstructionType.store, null);
            instructions.add(ainstruction);

            Assign = false;
        }


        if (tokenNow.getTokenType() != TokenType.SEMICOLON)
            throw new AnalyzeError(ErrorCode.NoSemicolon,tokenNow.getStartPos());   //读下一个token，应该是SEMICOLON
        tokenNow = Tokenizer.getToken();
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
        if (type.equals("int")||type.equals("char")) {
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


        //分析代码块
        analyseBlockStmt(type, 2,0,0);

        //若没有返回
        if (!hasReturn)
            throw new AnalyzeError(ErrorCode.noReturn,tokenNow.getStartPos());

        if (type.equals("void")) {
            Instruction ainstruction = new Instruction(InstructionType.ret, null);
            instructions.add(ainstruction);
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

    //function_param_list -> function_param (',' function_param)*
    public static void analyseFunctionParamList() throws Exception {
        analyseFunctionParam();
        while (tokenNow.getTokenType() == TokenType.COMMA) {
            tokenNow = Tokenizer.getToken();
            analyseFunctionParam();
        }
    }

    //function_param -> 'const'? IDENT ':' ty
    public static void analyseFunctionParam() throws Exception {
        if (tokenNow.getTokenType() == TokenType.CONST_KW)
            tokenNow=Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.IDENT)
            throw new AnalyzeError(ErrorCode.ExpectedToken,tokenNow.getStartPos());
        //处理参数
        String name = (String) tokenNow.getValue();

        tokenNow=Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.COLON)
            throw new AnalyzeError(ErrorCode.ExpectedToken,tokenNow.getStartPos());

        tokenNow=Tokenizer.getToken();
        String type = analyseTy();

        Param param = new Param(type, name);
        params.add(param);

    }

    //ty -> IDENT
    public static String analyseTy() throws Exception {
        //应该为IDENT
        if (tokenNow.getTokenType() != TokenType.IDENT)
            throw new AnalyzeError(ErrorCode.ExpectedToken,tokenNow.getStartPos());

        //获取类型
        String type = (String) tokenNow.getValue();

        //基础c0是int或void(另外再提醒一下，返回值类型 return_type 即使为 void 也不能省略)
        if (!(type.equals("int") || type.equals("void")||type.equals("char")))
            throw new AnalyzeError(ErrorCode.InvalidIdentifier,tokenNow.getStartPos());

        tokenNow = Tokenizer.getToken();

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
        if (tokenNow.getTokenType() == TokenType.MINUS) {
            Instruction negInstruction = new Instruction(InstructionType.neg, null);
            tokenNow=Tokenizer.getToken();
            if (tokenNow.getTokenType() == TokenType.MINUS) {
                analyseExpr(level);
                instructions.add(negInstruction);
            }
            else if (tokenNow.getTokenType() == TokenType.UINT_LITERAL) {
                analyseLiteralExpr();
                instructions.add(negInstruction);
                if (isOperator(tokenNow)) {
                    analyseOperatorExpr(level);
                }
            }
            else if (tokenNow.getTokenType() == TokenType.IDENT) {
                String name = (String) tokenNow.getValue();
                tokenNow=Tokenizer.getToken();
                if (tokenNow.getTokenType() == TokenType.L_PAREN) {
                    operatorStack.push(TokenType.L_PAREN);
                    if (!isNewFunction(name)) {
                        Integer id;
                        Instruction ainstruction;
                        // 是库函数
                        if (isStaticFunction(name)) {
//                            LibraryFunction function = new LibraryFunction(name, globalCount);
//                            libraryFunctions.add(function);
//                            id = globalCount;
//                            globalCount++;
//
//                            GlobalVar global = Format.functionNameToGlobalInformation(name);
//                            globals.add(global);
                            functionIntoGlobals(name);
                            ainstruction = new Instruction(InstructionType.callname, globalVarCount);
                            globalVarCount++;
                        }
                        //自定义函数
                        else {
                            id = getFunctionId(name);
                            ainstruction = new Instruction(InstructionType.call, id);
                        }
                        analyseCallExpr(name, level);

                        //弹栈
                        while (operatorStack.peek() != TokenType.L_PAREN) {
                            TokenType tokenType = operatorStack.pop();
                            operatorGetInstruction(tokenType, instructions);
                        }
                        operatorStack.pop();

                        instructions.add(ainstruction);
                    }else {
                        throw new AnalyzeError(ErrorCode.inValidFunction,tokenNow.getStartPos());
                    }
                    instructions.add(negInstruction);
                    if (isOperator(tokenNow)) {
                        analyseOperatorExpr(level);
                    }
                }else if (isOperator(tokenNow)) {
                    analyseIdentExpr(name, level);
                    instructions.add(negInstruction);
                    analyseOperatorExpr(level);
                }
                else {
                    analyseIdentExpr(name, level);
                    instructions.add(negInstruction);
                }
            }
        }
        else if (tokenNow.getTokenType() == TokenType.IDENT) {
            String name = (String) tokenNow.getValue();
            tokenNow = Tokenizer.getToken();
            if (tokenNow.getTokenType() == TokenType.ASSIGN) {
                if (Assign)
                    throw new AnalyzeError(ErrorCode.InvalidAssignment,tokenNow.getStartPos());

                //是变量或是参数
                if ((!isConst(name) && isVar(name)) || isParam(name)) {
                    //是局部变量
                    if (isLocal(name)) {
                        int id = getId(name, level);
                        //取出值
                        Instruction ainstruction = new Instruction(InstructionType.loca, id);
                        instructions.add(ainstruction);
                    }
                    //是参数
                    else if (isParam(name)) {
                        Integer id = getParamId(name);
                        //取出值
                        Instruction ainstruction = new Instruction(InstructionType.arga, address + id);
                        instructions.add(ainstruction);
                    }
                    else {
                        Integer id = getId(name, level);
                        Instruction ainstruction = new Instruction(InstructionType.globa, id);
                        instructions.add(ainstruction);
                    }
                    Assign = true;
                    analyseAssignExpr(name, level);
                }else {
                    throw new AnalyzeError(ErrorCode.InvalidAssignment,tokenNow.getStartPos());
                }
                Assign = false;
                if (isOperator(tokenNow)) {
                    analyseOperatorExpr(level);
                }
            }
            else if (tokenNow.getTokenType() == TokenType.L_PAREN) {
                operatorStack.push(TokenType.L_PAREN);
                if (!isNewFunction(name)) {
                    Integer id;
                    Instruction ainstruction;
                    // 是库函数
                    if (isStaticFunction(name)) {
//                        LibraryFunction function = new LibraryFunction(name, globalCount);
//                        libraryFunctions.add(function);
//                        id = globalCount;
//                        globalCount++;

//                        Global global = Format.functionNameToGlobalInformation(name);
//                        globals.add(global);
//                        instruction = new Instructions(Instruction.callname, id);
                        functionIntoGlobals(name);
                        ainstruction = new Instruction(InstructionType.callname, globalVarCount);
                        globalVarCount++;
                    }
                    //自定义函数
                    else {
                        id = getFunctionId(name);
                        ainstruction = new Instruction(InstructionType.call, id);
                    }
                    analyseCallExpr(name, level);

                    //弹栈
                    while (operatorStack.peek() != TokenType.L_PAREN) {
                        TokenType tokenType = operatorStack.pop();
                        operatorGetInstruction(tokenType, instructions);
                    }
                    operatorStack.pop();

                    instructions.add(ainstruction);

                }
                else {
                    throw new AnalyzeError(ErrorCode.inValidFunction,tokenNow.getStartPos());
                }
                if (isOperator(tokenNow)) {
                    analyseOperatorExpr(level);
                }
            }
            else if (isOperator(tokenNow)) {
                analyseIdentExpr(name, level);
                analyseOperatorExpr(level);
            }
            else if (tokenNow.getTokenType() == TokenType.AS_KW) {
                analyseAsExpr();
            }
            else {
                analyseIdentExpr(name, level);
            }
        }
        else if (tokenNow.getTokenType() == TokenType.UINT_LITERAL || tokenNow.getTokenType() == TokenType.STRING_LITERAL||tokenNow.getTokenType()==TokenType.CHAR_LITERAL) {
            analyseLiteralExpr();
            if (isOperator(tokenNow)) analyseOperatorExpr(level);
        }
        else if (tokenNow.getTokenType() == TokenType.L_PAREN) {
            operatorStack.push(TokenType.L_PAREN);
            analyseGroupExpr(level);
            if (isOperator(tokenNow)) {
                analyseOperatorExpr(level);
            }
        }
        else throw new AnalyzeError(ErrorCode.InvalidInput,tokenNow.getStartPos());
    }

    //block_stmt -> '{' stmt* '}'
    public static void analyseBlockStmt(String type, Integer level, Integer whileStart, Integer toWhileEnd) throws Exception {
        if (tokenNow.getTokenType() != TokenType.L_BRACE)
            throw new AnalyzeError(ErrorCode.noLB,tokenNow.getStartPos());

        //读下一个token
        tokenNow = Tokenizer.getToken();
        while (tokenNow.getTokenType() != TokenType.R_BRACE) {
            //分析语句：Stmt
            analyseStmt(type, level, whileStart, toWhileEnd);
        }
        //读下一个token
        tokenNow = Tokenizer.getToken();
    }

    //语句
    // stmt ->
    //      expr_stmt
    //    | decl_stmt
    //    | if_stmt
    //    | while_stmt
    //    | return_stmt
    //    | block_stmt
    //    | empty_stmt
    public static void analyseStmt(String type, Integer level,Integer whileStart,Integer toWhileEnd) throws Exception {
        if (tokenNow.getTokenType() == TokenType.CONST_KW || tokenNow.getTokenType() == TokenType.LET_KW)
            analyseDeclStmt(level);
        else if (tokenNow.getTokenType() == TokenType.IF_KW)
            analyseIfStmt(type, level,whileStart,toWhileEnd);
        else if (tokenNow.getTokenType() == TokenType.WHILE_KW)
            analyseWhileStmt(type, level);
        else if (tokenNow.getTokenType() == TokenType.RETURN_KW)
            analyseReturnStmt(type, level);
        else if (tokenNow.getTokenType() == TokenType.SEMICOLON)
            analyseEmptyStmt();
        else if (tokenNow.getTokenType() == TokenType.L_BRACE)
            analyseBlockStmt(type, level + 1,whileStart,toWhileEnd);
        else if (tokenNow.getTokenType() == TokenType.CONTINUE_KW)
            analyseContinueStmt(whileStart);
        else if (tokenNow.getTokenType() == TokenType.BREAK_KW)
            analyseBreakStmt(toWhileEnd);
        else
            analyseExprStmt(level);
    }

    //if_stmt -> 'if' expr block_stmt ('else' (block_stmt | if_stmt))?
    public static void analyseIfStmt(String type, Integer level,Integer whileStart,Integer toWhileEnd) throws Exception {
        if (tokenNow.getTokenType() != TokenType.IF_KW)
            throw new AnalyzeError(ErrorCode.noIf,tokenNow.getStartPos());

        //读下一个token，应该去分析expr
        tokenNow = Tokenizer.getToken();

        analyseExpr(level);
        //清空操作符栈（对应opg算法中读到'#'的操作）,并将与符号对应的指令填入指令集
        while (!operatorStack.empty()) {
            TokenType tokenType = operatorStack.pop();
            operatorGetInstruction(tokenType, instructions);
        }

        //brTrue，跳过br指令
        Instruction ainstruction = new Instruction(InstructionType.brTrue, 1);
        instructions.add(ainstruction);
        //br
        Instruction ifInstruction = new Instruction(InstructionType.br, 0);
        instructions.add(ifInstruction);
        int index = instructions.size();

        analyseBlockStmt(type, level + 1, whileStart, toWhileEnd);


        int size = instructions.size();


        if (instructions.get(size -1).getType().getValue() == 0x49) {
            int dis = instructions.size() - index;
            ifInstruction.setParam(dis);

            if (tokenNow.getTokenType() == TokenType.ELSE_KW) {
                //读下一个token，应该去分析有没有if
                tokenNow = Tokenizer.getToken();
                if (tokenNow.getTokenType() == TokenType.IF_KW)
                    analyseIfStmt(type, level,whileStart,toWhileEnd);
                else {
                    analyseBlockStmt(type, level + 1, whileStart, toWhileEnd);
//                    size = instructions.size();
                    ainstruction = new Instruction(InstructionType.br, 0);
                    instructions.add(ainstruction);
                }
            }
        }
        else {
            Instruction jumpInstruction = new Instruction(InstructionType.br, null);
            instructions.add(jumpInstruction);
            int jump = instructions.size();

            int dis = instructions.size() - index;
            ifInstruction.setParam(dis);

            if (tokenNow.getTokenType() == TokenType.ELSE_KW) {
                //读下一个token，应该去分析有没有if
                tokenNow = Tokenizer.getToken();
                if (tokenNow.getTokenType() == TokenType.IF_KW)
                    analyseIfStmt(type, level, whileStart, toWhileEnd);
                else {
                    analyseBlockStmt(type, level + 1, whileStart, toWhileEnd);
                    ainstruction = new Instruction(InstructionType.br, 0);
                    instructions.add(ainstruction);
                }
            }
            dis = instructions.size() - jump;
            jumpInstruction.setParam(dis);
        }
    }

    //while_stmt -> 'while' expr block_stmt
    public static void analyseWhileStmt(String type, Integer level) throws Exception {
        if (tokenNow.getTokenType() != TokenType.WHILE_KW)
            throw new AnalyzeError(ErrorCode.noWhile,tokenNow.getStartPos());

        Instruction ainstruction = new Instruction(InstructionType.br, 0);
        instructions.add(ainstruction);

        int whileStart = instructions.size();


        tokenNow=Tokenizer.getToken();
        analyseExpr(level);
        //弹栈
        while (!operatorStack.empty()) {
            TokenType tokenType = operatorStack.pop();
            operatorGetInstruction(tokenType,instructions);
        }

        //brTrue
        ainstruction = new Instruction(InstructionType.brTrue, 1);
        instructions.add(ainstruction);
        //br
        Instruction jumpInstruction = new Instruction(InstructionType.br, 0);
        instructions.add(jumpInstruction);
        int index = instructions.size();
        int toWhileEnd = index-1;


        //记录循环状态
        Circle = true;
        analyseBlockStmt(type, level + 1,whileStart,toWhileEnd);
        Circle = false;


        //跳至while 判断语句
        ainstruction = new Instruction(InstructionType.br, 0);
        instructions.add(ainstruction);
        int whileEnd = instructions.size();
        int dis = whileStart - whileEnd;
        ainstruction.setParam(dis);


        dis = instructions.size() - index;
        jumpInstruction.setParam(dis);

    }

    //return_stmt -> 'return' expr? ';'
    public static void analyseReturnStmt(String type, Integer level) throws Exception {
        if (tokenNow.getTokenType() != TokenType.RETURN_KW)
            throw new AnalyzeError(ErrorCode.noReturn,tokenNow.getStartPos());

        tokenNow=Tokenizer.getToken();
        if (tokenNow.getTokenType()!= TokenType.SEMICOLON) {
            if (type.equals("int")||type.equals("char")) {
                //取返回地址
                Instruction ainstruction = new Instruction(InstructionType.arga, 0);
                instructions.add(ainstruction);

                analyseExpr(level);

                while (!operatorStack.empty()) {
                    operatorGetInstruction(operatorStack.pop(),instructions);
                }
                //放入地址中
                ainstruction = new Instruction(InstructionType.store, null);
                instructions.add(ainstruction);
                hasReturn = true;
            }
            else if (type.equals("void"))
                throw new AnalyzeError(ErrorCode.noReturn,tokenNow.getStartPos());
        }
        if (tokenNow.getTokenType() != TokenType.SEMICOLON)
            throw new AnalyzeError(ErrorCode.NoSemicolon,tokenNow.getStartPos());
        while (!operatorStack.empty()) {
            operatorGetInstruction(operatorStack.pop(),instructions);
        }
        //rett
        Instruction ainstruction = new Instruction(InstructionType.ret, null);
        instructions.add(ainstruction);
        tokenNow=Tokenizer.getToken();
    }

    //empty_stmt -> ';'
    public static void analyseEmptyStmt() throws Exception {
        if (tokenNow.getTokenType() != TokenType.SEMICOLON)
            throw new AnalyzeError(ErrorCode.NoSemicolon,tokenNow.getStartPos());

        tokenNow=Tokenizer.getToken();
    }

    //continue_stmt -> 'continue' ';'
    public static void analyseContinueStmt(Integer whileStart) throws Exception{
        //不在循环中出现
        if(!Circle)
            throw new AnalyzeError(ErrorCode.noInCircle,tokenNow.getStartPos());
        if(tokenNow.getTokenType() != TokenType.CONTINUE_KW)
            throw new AnalyzeError(ErrorCode.noContinue,tokenNow.getStartPos());
        //跳转语句
        Instruction ainstruction = new Instruction(InstructionType.br, 0);
        instructions.add(ainstruction);

        Integer dis = whileStart-instructions.size();
        ainstruction.setParam(dis);

        tokenNow=Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.SEMICOLON)
            throw new AnalyzeError(ErrorCode.NoSemicolon,tokenNow.getStartPos());
    }

    //break_stmt -> 'break' ';'
    public static void analyseBreakStmt(Integer toWhileEnd) throws Exception{
        //不在循环中出现
        if(!Circle)
            throw new AnalyzeError(ErrorCode.noInCircle,tokenNow.getStartPos());
        if(tokenNow.getTokenType() != TokenType.BREAK_KW)
            throw new AnalyzeError(ErrorCode.noBreak,tokenNow.getStartPos());
        //跳转语句
        Instruction ainstruction = new Instruction(InstructionType.br, 0);
        instructions.add(ainstruction);

        Integer dis = toWhileEnd-instructions.size()+1;
        ainstruction.setParam(dis);

        tokenNow=Tokenizer.getToken();
        if (tokenNow.getTokenType() != TokenType.SEMICOLON)
            throw new AnalyzeError(ErrorCode.NoSemicolon,tokenNow.getStartPos());
    }

    //expr_stmt -> expr ';'
    public static void analyseExprStmt(Integer level) throws Exception {
        analyseExpr(level);
        //清空操作符栈（对应opg算法中读到'#'的操作）,并将与符号对应的指令填入指令集
        while (!operatorStack.empty()) {
            TokenType tokenType = operatorStack.pop();
            operatorGetInstruction(tokenType, instructions);
        }
        if (tokenNow.getTokenType() != TokenType.SEMICOLON)
            throw new AnalyzeError(ErrorCode.NoSemicolon,tokenNow.getStartPos());

        tokenNow=Tokenizer.getToken();
    }

    //literal_expr -> UINT_LITERAL | DOUBLE_LITERAL | STRING_LITERAL | CHAR_LITERAL
    public static void analyseLiteralExpr() throws Exception {
        if (tokenNow.getTokenType() == TokenType.UINT_LITERAL) {
            //加载常数
            Instruction ainstruction = new Instruction(InstructionType.push, (Integer) tokenNow.getValue());
            instructions.add(ainstruction);
        }
        else if (tokenNow.getTokenType() == TokenType.STRING_LITERAL) {
            //填入全局符号表
            functionIntoGlobals((String)tokenNow.getValue());

            //加入指令集
            Instruction ainstruction = new Instruction(InstructionType.push, globalVarCount);
            instructions.add(ainstruction);
            globalVarCount++;

        }
        else if(tokenNow.getTokenType()==TokenType.CHAR_LITERAL){
            //加载字符
            Instruction ainstruction = new Instruction(InstructionType.push, (Integer)tokenNow.getValue());
            instructions.add(ainstruction);

        }
        else
            throw new AnalyzeError(ErrorCode.ExpectedToken,tokenNow.getStartPos());

        tokenNow=Tokenizer.getToken();
    }

    //operator_expr -> expr binary_operator expr
    public static void analyseOperatorExpr(Integer level) throws Exception {
        if (!operatorStack.empty()) {
            int in = getpos(operatorStack.peek());
            int out = getpos(tokenNow.getTokenType());
            if (priority[in][out] > 0) {
                TokenType type = operatorStack.pop();
                operatorGetInstruction(type, instructions);
            }
        }
        operatorStack.push(tokenNow.getTokenType());

        tokenNow=Tokenizer.getToken();
        analyseExpr(level);
    }

    //call_expr -> IDENT '(' call_param_list? ')'
    public static void analyseCallExpr(String name, Integer level) throws Exception {
        Instruction ainstruction;
        int paracount = 0; //参数个数
        //分配返回值空间
        if (hasReturn(name)) {
            ainstruction = new Instruction(InstructionType.stackalloc, 1);
        }
        else {
            if (Assign)
                throw new AnalyzeError(ErrorCode.InvalidAssignment,tokenNow.getStartPos());
            ainstruction = new Instruction(InstructionType.stackalloc, 0);
        }

        instructions.add(ainstruction);

        tokenNow=Tokenizer.getToken();

        if (tokenNow.getTokenType() != TokenType.R_PAREN)
            paracount = analyseCallParamList(level);

        if (!checkParamNum(name, paracount))
            throw new AnalyzeError(ErrorCode.invalidParam,tokenNow.getStartPos());

        if (tokenNow.getTokenType() != TokenType.R_PAREN)
            throw new AnalyzeError(ErrorCode.noRP,tokenNow.getStartPos());

        tokenNow=Tokenizer.getToken();
    }

    //ident_expr -> IDENT
    public static void analyseIdentExpr(String name, Integer level) throws Exception {
        //不是常量、变量、参数
        if (!(isVar(name) || isConst(name) || isParam(name)))
            throw new AnalyzeError(ErrorCode.NotDeclared,tokenNow.getStartPos());
        Instruction ainstruction;
        //局部变量
        int id;
        if (isLocal(name)) {
            id = getId(name, level);
            ainstruction = new Instruction(InstructionType.loca, id);
            instructions.add(ainstruction);
        }
        //参数
        else if (isParam(name)) {
            id = getParamId(name);
            ainstruction = new Instruction(InstructionType.arga, address + id);
            instructions.add(ainstruction);
        }
        //全局变量
        else {
            id = getId(name, level);
            ainstruction = new Instruction(InstructionType.globa, id);
            instructions.add(ainstruction);
        }
        ainstruction = new Instruction(InstructionType.load, null);
        instructions.add(ainstruction);
    }

    //call_param_list -> expr (',' expr)*
    public static int analyseCallParamList(int level) throws Exception {
        analyseExpr(level);
        while (!operatorStack.empty() && operatorStack.peek() != TokenType.L_PAREN) {
            operatorGetInstruction(operatorStack.pop(),instructions);
        }
        int paracount = 1;
        while (tokenNow.getTokenType() == TokenType.COMMA) {
            tokenNow=Tokenizer.getToken();
            analyseExpr(level);
            while (!operatorStack.empty() && operatorStack.peek() != TokenType.L_PAREN) {
                operatorGetInstruction(operatorStack.pop(),instructions);
            }
            paracount++;
        }
        return paracount;
    }

    //assign_expr -> l_expr '=' expr
    public static void analyseAssignExpr(String name, Integer level) throws Exception {
        tokenNow=Tokenizer.getToken();
        analyseExpr(level);
        while (!operatorStack.empty()) {
            operatorGetInstruction(operatorStack.pop(),instructions);
        }
        //存储到地址中
        Instruction ainstruction = new Instruction(InstructionType.store, null);
        instructions.add(ainstruction);
    }

    //as_expr -> expr 'as' ty
    public static void analyseAsExpr() throws Exception {
        tokenNow=Tokenizer.getToken();
        String type = analyseTy();
        if (!(type.equals("int")||type.equals("char")))
            throw new AnalyzeError(ErrorCode.invalidType,tokenNow.getStartPos());
    }

    //group_expr -> '(' expr ')'
    public static void analyseGroupExpr(Integer level) throws Exception {
        if (tokenNow.getTokenType() != TokenType.L_PAREN)
            throw new AnalyzeError(ErrorCode.noLP,tokenNow.getStartPos());

        tokenNow=Tokenizer.getToken();
        analyseExpr(level);


        if (tokenNow.getTokenType() != TokenType.R_PAREN)
            throw new AnalyzeError(ErrorCode.noRP,tokenNow.getStartPos());

        while (operatorStack.peek() != TokenType.L_PAREN) {
            operatorGetInstruction(operatorStack.pop(),instructions);
        }
        operatorStack.pop();

        tokenNow=Tokenizer.getToken();
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
        Instruction ainstruction;
        switch (type) {
            case MINUS:
                ainstruction = new Instruction(InstructionType.sub, null);
                instructionsList.add(ainstruction);
                break;
            case MUL:
                ainstruction = new Instruction(InstructionType.mul, null);
                instructionsList.add(ainstruction);
                break;
            case DIV:
                ainstruction = new Instruction(InstructionType.div, null);
                instructionsList.add(ainstruction);
                break;
            case EQ:
                ainstruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(ainstruction);
                ainstruction = new Instruction(InstructionType.not, null);
                instructionsList.add(ainstruction);
                break;
            case NEQ:
                ainstruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(ainstruction);
                break;
            case LT:
                ainstruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(ainstruction);
                ainstruction = new Instruction(InstructionType.setLt, null);
                instructionsList.add(ainstruction);
                break;
            case GT:
                ainstruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(ainstruction);
                ainstruction = new Instruction(InstructionType.setGt, null);
                instructionsList.add(ainstruction);
                break;
            case LE:
                ainstruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(ainstruction);
                ainstruction = new Instruction(InstructionType.setGt, null);
                instructionsList.add(ainstruction);
                ainstruction = new Instruction(InstructionType.not, null);
                instructionsList.add(ainstruction);
                break;
            case GE:
                ainstruction = new Instruction(InstructionType.cmp, null);
                instructionsList.add(ainstruction);
                ainstruction = new Instruction(InstructionType.setLt, null);
                instructionsList.add(ainstruction);
                ainstruction = new Instruction(InstructionType.not, null);
                instructionsList.add(ainstruction);
                break;
            case PLUS:
                ainstruction = new Instruction(InstructionType.add, null);
                instructionsList.add(ainstruction);
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

    //查常量表
    public static boolean isConst(String name) {
        for (Const constant : Consts) {
            if (constant.getName().equals(name)) return true;
        }
        return false;
    }

    //查变量表
    public static boolean isVar(String name) {
        for (Var variable : Vars) {
            if (variable.getName().equals(name)) return true;
        }
        return false;
    }

    //查局部变量表
    public static boolean isLocal(String name) {
        for (Const constant : Consts) {
            if (constant.getName().equals(name) && constant.getLevel() > 1)
                return true;
        }
        for (Var variable : Vars) {
            if (variable.getName().equals(name) && variable.getLevel() > 1)
                return true;
        }
        return false;
    }

    //查参数表
    public static boolean isParam(String name) {
        for (Param param : params) {
            if (param.getName().equals(name))
                return true;
        }
        return false;
    }

    //获得id
    public static int getId(String name, int level) {
        int len = Vars.size();
        for (int i = len - 1; i >= 0; --i) {
            Var variable = Vars.get(i);
            if (variable.getName().equals(name) && variable.getLevel() <= level)
                return variable.getId();
        }
        len = Consts.size();
        for (int i = len - 1; i >= 0; --i) {
            Const constant = Consts.get(i);
            if (constant.getName().equals(name) && constant.getLevel() <= level)
                return constant.getId();
        }
        return -1;
    }

    //获得参数id
    public static int getParamId(String name) {
        for (int i = 0; i < params.size(); ++i) {
            if (params.get(i).getName().equals(name))
                return i;
        }
        return -1;
    }

    //添加函数为全局变量
    public static void functionIntoGlobals(String name) {
        char[] arr = name.toCharArray();
        int len = arr.length;
        GlobalVar global = new GlobalVar(true,len, name);
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

    //查看是否为操作符
    public static boolean isOperator(Token token) {
        if (token.getTokenType() != TokenType.PLUS &&
                token.getTokenType() != TokenType.MINUS &&
                token.getTokenType() != TokenType.MUL &&
                token.getTokenType() != TokenType.DIV &&
                token.getTokenType() != TokenType.EQ &&
                token.getTokenType() != TokenType.NEQ &&
                token.getTokenType() != TokenType.LE &&
                token.getTokenType() != TokenType.LT &&
                token.getTokenType() != TokenType.GE &&
                token.getTokenType() != TokenType.GT)
            return false;
        return true;
    }

    //查看算符索引
    public static int getpos(TokenType tokenType){
        if(tokenType== TokenType.PLUS){
            return 0;
        }
        else if(tokenType== TokenType.MINUS){
            return 1;
        }
        else if(tokenType== TokenType.MUL){
            return 2;
        }
        else if(tokenType== TokenType.DIV){
            return 3;
        }
        else if(tokenType== TokenType.L_PAREN){
            return 4;
        }
        else if(tokenType== TokenType.R_PAREN){
            return 5;
        }
        else if(tokenType== TokenType.LT){
            return 6;
        }
        else if(tokenType== TokenType.GT){
            return 7;
        }
        else if(tokenType== TokenType.LE){
            return 8;
        }
        else if(tokenType== TokenType.GE){
            return 9;
        }
        else if(tokenType== TokenType.EQ){
            return 10;
        }
        else if(tokenType== TokenType.NEQ){
            return 11;
        }
        return -1;
    }

    //查看函数索引
    public static int getFunctionId(String name){
        for (Function function : Functions) {
            if (function.getFunction_name().equals(name))
                return function.getId();
        }
        return -1;
    }

    //查看函数是否有返回值
    public static boolean hasReturn(String name) {
        if (isStaticFunction(name)) {
            if (name.equals("getint") || name.equals("getdouble") || name.equals("getchar")) {
                return true;
            } else return false;
        }
        for (Function function : Functions) {
            if (function.getFunction_name().equals(name)) {
                if (function.getReturn_type().equals("int")||function.getReturn_type().equals("char")) return true;
            }
        }
        return false;
    }

    //检查参数个数是否正确
    public static boolean checkParamNum(String name,int num) {
        if (isStaticFunction(name)) {
            if (name.equals("getint") || name.equals("getdouble") || name.equals("getchar") || name.equals("putln")) {
                return num == 0;
            } else return num == 1;
        }
        for (Function function : Functions) {
            if (function.getFunction_name().equals(name)) {
                if (num == function.getParam_list().size()) return true;
            }
        }
        return false;
    }

    //是否有main函数
    public static boolean hasMain() {
        for (Function function : Functions) {
            if (function.getFunction_name().equals("main"))
                return true;
        }
        return false;
    }

    //main函数是否有返回值
    public static boolean mainHasReturn() {
        for (Function function : Functions) {
            if (function.getFunction_name().equals("main")) {
                if (function.getReturn_type().equals("int")||function.getReturn_type().equals("char")) return true;
                break;
            }
        }
        return false;
    }

    public static Tokenizer getTokenizer() {
        return tokenizer;
    }

    public static Token getTokenNow() {
        return tokenNow;
    }

    public static List<Instruction> getInstructions() {
        return instructions;
    }

    public static List<Var> getVars() {
        return Vars;
    }

    public static List<Const> getConsts() {
        return Consts;
    }

    public static List<Function> getFunctions() {
        return Functions;
    }

    public static List<GlobalVar> getGlobalVars() {
        return globalVars;
    }

    public static List<Param> getParams() {
        return params;
    }

    public static FunctionWithInstructions getStartFunction() {
        return startFunction;
    }

    public static int getLocalVarCount() {
        return localVarCount;
    }

    public static int getGlobalVarCount() {
        return globalVarCount;
    }

    public static int getFunctionCount() {
        return functionCount;
    }

    public static Stack<TokenType> getOperatorStack() {
        return operatorStack;
    }

    public static boolean isAssign() {
        return Assign;
    }

    public static boolean isHasReturn() {
        return hasReturn;
    }

    public static int getAddress() {
        return address;
    }

    public static List<FunctionWithInstructions> getFunctionWithInstructionsList() {
        return FunctionWithInstructionsList;
    }

    public static int[][] getPriority() {
        return priority;
    }
}
