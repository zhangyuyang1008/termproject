package tokenizer;

import error.ErrorCode;
import error.TokenizeError;
import util.Pos;

import java.io.InputStream;
import java.text.Format;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

import static error.ErrorCode.IntegerOverflow;

public class Tokenizer {

    //自定义字符迭代器
    private StringIter it;

    String token="";

    //token列表
    private static List<Token> tokenList;
    //token迭代器
    private static Iterator<Token> tokenIterator;

    public Tokenizer(StringIter it) {
        this.it = it;
    }

    // 这里本来是想实现 Iterator<Token> 的，但是 Iterator 不允许抛异常，于是就这样了

    //运行词法分析
    public static void runTokenizer(Scanner s) throws Exception {
        tokenList = new ArrayList<>();
        Tokenizer tokenizer = new Tokenizer(new StringIter(s));
        Token tokenNow = tokenizer.nextToken();
        while (!tokenNow.getTokenType().toString().equals("EOF")) {
            tokenList.add(tokenNow);
            System.out.print(tokenNow.toString() + "\n");
            tokenNow = tokenizer.nextToken();
        }
        initIterator();
    }

    /**
     * 获取下一个 Token
     *
     * @return
     * @throws TokenizeError 如果解析有异常则抛出
     */
    public Token nextToken() throws TokenizeError {
        // Scanner一次读入全部内容，并且替换所有换行为 \n
        it.readAll();

        // 跳过之前的所有空白字符
        skipSpaceCharacters();

        if (it.isEOF()) {
            return new Token(TokenType.EOF, "", it.currentPos(), it.currentPos());
        }
        //偷看第一个字符
        char peek = it.peekChar();
        //如果是数字则调用分析无符号整数token的函数
        if (Character.isDigit(peek)) {
            return Unit_literal();
        }
        //如果是字母或下划线则调用分析标识符或关键字token的函数
        else if (Character.isAlphabetic(peek)||peek=='_') {
            return IdentOrKeyword();
        }
        else if (peek=='"') {
            return Str();
        }
        //都不是则调用分析运算符token的喊数
        else {
            return Operator();
        }
    }

    //分析无符号整数
    private Token Unit_literal() throws TokenizeError {
        token="";
        //记录初始位置
        Pos p=it.ptr;
        char charNow = it.nextChar();
        // 将字符串和字符连接起来
        token+=charNow;
        // 查看下一个字符 但是不移动指针
        charNow = it.peekChar();
        while(Character.isDigit(charNow)){
            // 还是数字，前进一个字符，并存储这个字符
            charNow=it.nextChar();
            // 将字符串和字符连接起来
            token+=charNow;
            // 查看下一个字符 但是不移动指针
            charNow = it.peekChar();
        }
        // 解析存储的字符串为无符号整数
        try{
            Integer num = Integer.parseInt(token);
            return new Token(TokenType.UINT_LITERAL, num,p, it.ptr);
        }
        catch (Exception e){
            throw new TokenizeError(ErrorCode.IntegerOverflow,p);
        }
        // 解析成功则返回无符号整数类型的token，否则返回编译错误
        //
        // Token 的 Value 应填写数字的值
    }


    //分析标识符或关键字
    private Token IdentOrKeyword() throws TokenizeError {
        token="";
        //记录初始位置
        Pos p=it.ptr;
        // 前进一个字符，并存储这个字符
        char charNow = it.nextChar();
        // 将字符串和字符连接起来
        token+=charNow;

        // 直到查看下一个字符不是数字或字母为止:
        charNow = it.peekChar();
        while(Character.isDigit(charNow) || Character.isAlphabetic(charNow)||charNow=='_'){
            // 前进一个字符，并存储这个字符
            charNow=it.nextChar();
            // 将字符串和字符连接起来
            token+=charNow;
            // 查看下一个字符 但是不移动指针
            charNow = it.peekChar();
        }
        //
        // 尝试将存储的字符串解释为关键字
        // -- 如果是关键字，则返回关键字类型的 token
        // -- 否则，返回标识符
        // Token 的 Value 应填写标识符或关键字的字符串
        try{
            if(token.toLowerCase().equals("fn")){
                return new Token(TokenType.FN_KW, "fn", p, it.ptr);
            }
            else if (token.toLowerCase().equals("let")){
                return new Token(TokenType.LET_KW, "let", p, it.ptr);
            }
            else if (token.toLowerCase().equals("as")){
                return new Token(TokenType.AS_KW, "as", p, it.ptr);
            }
            else if (token.toLowerCase().equals("const")){
                return new Token(TokenType.CONST_KW, "const", p, it.ptr);
            }
            else if (token.toLowerCase().equals("while")){
                return new Token(TokenType.WHILE_KW, "while", p, it.ptr);

            }
            else if (token.toLowerCase().equals("if")){
                return new Token(TokenType.IF_KW, "if", p, it.ptr);

            }
            else if (token.toLowerCase().equals("else")){
                return new Token(TokenType.ELSE_KW, "else", p, it.ptr);

            }
            else if (token.toLowerCase().equals("return")){
                return new Token(TokenType.RETURN_KW, "return", p, it.ptr);
            }
            else {
                return new Token(TokenType.IDENT, token, p, it.ptr);
            }
        }
        catch (Exception e){
            throw new TokenizeError(ErrorCode.InvalidInput,it.ptr);
        }
    }

    //分析字符串
    private Token Str() throws TokenizeError{
        token="";
        //记录初始位置
        Pos p=it.ptr;
//        // 前进一个字符，并存储这个字符
//        char charNow = it.nextChar();
//        // 将字符串和字符连接起来
//        token+=charNow;
        //指向"
        it.nextChar();
        // 偷看下一个字符
        char peek = it.peekChar();
        //定义charNow
        char charNow = 0;
        while (true){
            if (peek == '\\') {
                //再前进一个字符
                it.nextChar();
                peek = it.peekChar();
                switch (peek) {
                    case '\'':
                        charNow =  '\'';
                        break;
                    case '\"':
                        charNow = '\"';
                        break;
                    case '\\':
                        charNow = '\\';
                        break;
                    case 'n':
                        charNow = '\n';
                        break;
                    case 'r':
                        charNow = '\r';
                        break;
                    case 't':
                        charNow = '\t';
                        break;
                    default:
                        throw new TokenizeError(ErrorCode.InvalidInput,it.ptr);
                    }
            }
            else if(peek == '\"'){
                it.nextChar();
                break;
            }
            else charNow = peek;
            token += charNow;
            it.nextChar();
            peek = it.peekChar();
        }
        return new Token(TokenType.STRING_LITERAL, token, p, it.ptr);
    }

    //分析运算符
    private Token Operator() throws TokenizeError {
        //指向当前正在分析的符号
        switch (it.nextChar()) {
            case '+':
                try{
                    return new Token(TokenType.PLUS, '+', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }

            case '-':
                try{
                    if(it.peekChar()=='>'){
                    //向前移动到第二个等号
                    it.nextChar();
                    return new Token(TokenType.ARROW, "->", it.previousPos(), it.currentPos());
                }
                else return new Token(TokenType.MINUS, '-', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }

            case '*':
                try{
                    return new Token(TokenType.MUL, '*', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }

            case '/':
                try{
                    return new Token(TokenType.DIV, '/', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case '=':
                try{
                    if(it.peekChar()=='='){
                        //向前移动到第二个等号
                        it.nextChar();
                        return new Token(TokenType.EQ, "==", it.previousPos(), it.currentPos());
                    }
                    else return new Token(TokenType.ASSIGN, '=', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case '!':
                try{
                    return new Token(TokenType.NEQ, "!=", it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case '<':
                try{
                    if(it.peekChar()=='='){
                        //向前移动到第二个等号
                        it.nextChar();
                        return new Token(TokenType.LE, "<=", it.previousPos(), it.currentPos());
                    }
                    else return new Token(TokenType.LT, '<', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case '>':
                try{
                    if(it.peekChar()=='='){
                        //向前移动到第二个等号
                        it.nextChar();
                        return new Token(TokenType.GE, ">=", it.previousPos(), it.currentPos());
                    }
                    else return new Token(TokenType.GT, '>', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case ';':
                try{
                    return new Token(TokenType.SEMICOLON, ';', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case '(':
                try{
                    return new Token(TokenType.L_PAREN, '(', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case ')':
                try{
                    return new Token(TokenType.R_PAREN, ')', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case '{':
                try{
                    return new Token(TokenType.L_BRACE, '{', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case '}':
                try{
                    return new Token(TokenType.R_BRACE, '}', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case ',':
                try{
                    return new Token(TokenType.COMMA, ',', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }
            case ':':
                try{
                    return new Token(TokenType.COLON, ':', it.previousPos(), it.currentPos());
                }catch (Exception e){
                    throw new TokenizeError(ErrorCode.ExpectedToken,it.previousPos());
                }

            default:
                // 不认识这个输入，摸了
                throw new TokenizeError(ErrorCode.InvalidInput, it.previousPos());
        }
    }

    //跳过开头空白符
    private void skipSpaceCharacters() {

        while (!it.isEOF() && Character.isWhitespace(it.peekChar())) {
            it.nextChar();
        }
    }

    //初始化迭代器
    public static void initIterator() {
        tokenIterator = tokenList.iterator();
    }

    //获取token
    public static Token getToken() {
        if (tokenIterator.hasNext()) {
            Token token = tokenIterator.next();
            return token;
        }
        return null;
    }
}
