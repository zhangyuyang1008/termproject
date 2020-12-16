package error;

public enum ErrorCode {
    NoError, // Should be only used internally.
    StreamError, EOF, InvalidInput, InvalidIdentifier, IntegerOverflow, // int32_t overflow.
    NoBegin, NoEnd, NeedIdentifier, ConstantNeedValue, NoSemicolon, InvalidVariableDeclaration, IncompleteExpression,
    NotDeclared, AssignToConstant, DuplicateDeclaration, NotInitialized, InvalidAssignment, InvalidPrint, ExpectedToken,
    noConstant,noColon,noInt,noLet,noLP,noRP,noArrow,noReturn,noLB,noRB,noIf,noWhile,inValidFunction,invalidParam,
    invalidType,noMain,noInCircle,noContinue,noBreak

}
