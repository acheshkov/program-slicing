protected void consumeToken(int type) {
    /* remember the last consumed value */
    /* try to minimize the number of build values */
    //	// clear the commentPtr of the scanner in case we read something different from a modifier
    //	switch(type) {
    //		case TokenNameabstract :
    //		case TokenNamestrictfp :
    //		case TokenNamefinal :
    //		case TokenNamenative :
    //		case TokenNameprivate :
    //		case TokenNameprotected :
    //		case TokenNamepublic :
    //		case TokenNametransient :
    //		case TokenNamevolatile :
    //		case TokenNamestatic :
    //		case TokenNamesynchronized :
    //			break;
    //		default:
    //			this.scanner.commentPtr = -1;
    //	}
    //System.out.println(this.scanner.toStringAction(type));
    switch (type) {
    case TokenNameIdentifier :
        pushIdentifier();
        if (this.scanner.useAssertAsAnIndentifier  &&
                this.lastErrorEndPositionBeforeRecovery < this.scanner.currentPosition) {
            long positions = this.identifierPositionStack[this.identifierPtr];
            if(!this.statementRecoveryActivated) problemReporter().useAssertAsAnIdentifier((int) (positions >>> 32), (int) positions);
        }
        if (this.scanner.useEnumAsAnIndentifier  &&
                this.lastErrorEndPositionBeforeRecovery < this.scanner.currentPosition) {
            long positions = this.identifierPositionStack[this.identifierPtr];
            if(!this.statementRecoveryActivated) problemReporter().useEnumAsAnIdentifier((int) (positions >>> 32), (int) positions);
        }
        break;
    case TokenNameinterface :
        //'class' is pushing two int (positions) on the stack ==> 'interface' needs to do it too....
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNameabstract :
        checkAndSetModifiers(ClassFileConstants.AccAbstract);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNamestrictfp :
        checkAndSetModifiers(ClassFileConstants.AccStrictfp);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNamefinal :
        checkAndSetModifiers(ClassFileConstants.AccFinal);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNamenative :
        checkAndSetModifiers(ClassFileConstants.AccNative);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNameprivate :
        checkAndSetModifiers(ClassFileConstants.AccPrivate);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNameprotected :
        checkAndSetModifiers(ClassFileConstants.AccProtected);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNamepublic :
        checkAndSetModifiers(ClassFileConstants.AccPublic);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNametransient :
        checkAndSetModifiers(ClassFileConstants.AccTransient);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNamevolatile :
        checkAndSetModifiers(ClassFileConstants.AccVolatile);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNamestatic :
        checkAndSetModifiers(ClassFileConstants.AccStatic);
        pushOnExpressionStackLengthStack(0);
        break;
    case TokenNamesynchronized :
        this.synchronizedBlockSourceStart = this.scanner.startPosition;
        checkAndSetModifiers(ClassFileConstants.AccSynchronized);
        pushOnExpressionStackLengthStack(0);
        break;
    //==============================
    case TokenNamevoid :
        pushIdentifier(-T_void);
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    //push a default dimension while void is not part of the primitive
    //declaration baseType and so takes the place of a type without getting into
    //regular type parsing that generates a dimension on this.intStack
    case TokenNameboolean :
        pushIdentifier(-T_boolean);
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNamebyte :
        pushIdentifier(-T_byte);
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNamechar :
        pushIdentifier(-T_char);
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNamedouble :
        pushIdentifier(-T_double);
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNamefloat :
        pushIdentifier(-T_float);
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNameint :
        pushIdentifier(-T_int);
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNamelong :
        pushIdentifier(-T_long);
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNameshort :
        pushIdentifier(-T_short);
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    //==============================
    case TokenNameIntegerLiteral :
        pushOnExpressionStack(
            new IntLiteral(
                this.scanner.getCurrentTokenSource(),
                this.scanner.startPosition,
                this.scanner.currentPosition - 1));
        break;
    case TokenNameLongLiteral :
        pushOnExpressionStack(
            new LongLiteral(
                this.scanner.getCurrentTokenSource(),
                this.scanner.startPosition,
                this.scanner.currentPosition - 1));
        break;
    case TokenNameFloatingPointLiteral :
        pushOnExpressionStack(
            new FloatLiteral(
                this.scanner.getCurrentTokenSource(),
                this.scanner.startPosition,
                this.scanner.currentPosition - 1));
        break;
    case TokenNameDoubleLiteral :
        pushOnExpressionStack(
            new DoubleLiteral(
                this.scanner.getCurrentTokenSource(),
                this.scanner.startPosition,
                this.scanner.currentPosition - 1));
        break;
    case TokenNameCharacterLiteral :
        pushOnExpressionStack(
            new CharLiteral(
                this.scanner.getCurrentTokenSource(),
                this.scanner.startPosition,
                this.scanner.currentPosition - 1));
        break;
    case TokenNameStringLiteral :
        StringLiteral stringLiteral;
        if (this.recordStringLiterals && this.checkExternalizeStrings && !this.statementRecoveryActivated) {
            stringLiteral = this.createStringLiteral(
                                this.scanner.getCurrentTokenSourceString(),
                                this.scanner.startPosition,
                                this.scanner.currentPosition - 1,
                                Util.getLineNumber(this.scanner.startPosition, this.scanner.lineEnds, 0, this.scanner.linePtr));
            this.compilationUnit.recordStringLiteral(stringLiteral);
        } else {
            stringLiteral = this.createStringLiteral(
                                this.scanner.getCurrentTokenSourceString(),
                                this.scanner.startPosition,
                                this.scanner.currentPosition - 1,
                                0);
        }
        pushOnExpressionStack(stringLiteral);
        break;
    case TokenNamefalse :
        pushOnExpressionStack(
            new FalseLiteral(this.scanner.startPosition, this.scanner.currentPosition - 1));
        break;
    case TokenNametrue :
        pushOnExpressionStack(
            new TrueLiteral(this.scanner.startPosition, this.scanner.currentPosition - 1));
        break;
    case TokenNamenull :
        pushOnExpressionStack(
            new NullLiteral(this.scanner.startPosition, this.scanner.currentPosition - 1));
        break;
    //============================
    case TokenNamesuper :
    case TokenNamethis :
        this.endPosition = this.scanner.currentPosition - 1;
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNameassert :
    case TokenNameimport :
    case TokenNamepackage :
    case TokenNamethrow :
    case TokenNamedo :
    case TokenNameif :
    case TokenNamefor :
    case TokenNameswitch :
    case TokenNametry :
    case TokenNamewhile :
    case TokenNamebreak :
    case TokenNamecontinue :
    case TokenNamereturn :
    case TokenNamecase :
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNamenew :
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=40954
        resetModifiers();
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNameclass :
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNameenum :
        pushOnIntStack(this.scanner.currentPosition - 1);
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNamedefault :
        pushOnIntStack(this.scanner.startPosition);
        pushOnIntStack(this.scanner.currentPosition - 1);
        break;
    //let extra semantic action decide when to push
    case TokenNameRBRACKET :
        this.endPosition = this.scanner.startPosition;
        this.endStatementPosition = this.scanner.currentPosition - 1;
        break;
    case TokenNameLBRACE :
        this.endStatementPosition = this.scanner.currentPosition - 1;
    case TokenNamePLUS :
    case TokenNameMINUS :
    case TokenNameNOT :
    case TokenNameTWIDDLE :
        this.endPosition = this.scanner.startPosition;
        break;
    case TokenNamePLUS_PLUS :
    case TokenNameMINUS_MINUS :
        this.endPosition = this.scanner.startPosition;
        this.endStatementPosition = this.scanner.currentPosition - 1;
        break;
    case TokenNameRBRACE:
    case TokenNameSEMICOLON :
        this.endStatementPosition = this.scanner.currentPosition - 1;
        this.endPosition = this.scanner.startPosition - 1;
        //the item is not part of the potential futur expression/statement
        break;
    case TokenNameRPAREN :
        // in order to handle ( expression) ////// (cast)expression///// foo(x)
        this.rParenPos = this.scanner.currentPosition - 1; // position of the end of right parenthesis (in case of unicode \u0029) lex00101
        break;
    case TokenNameLPAREN :
        this.lParenPos = this.scanner.startPosition;
        break;
    case TokenNameAT :
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNameQUESTION  :
        pushOnIntStack(this.scanner.startPosition);
        pushOnIntStack(this.scanner.currentPosition - 1);
        break;
    case TokenNameLESS :
        pushOnIntStack(this.scanner.startPosition);
        break;
    case TokenNameELLIPSIS :
        pushOnIntStack(this.scanner.currentPosition - 1);
        break;
        //  case TokenNameCOMMA :
        //  case TokenNameCOLON  :
        //  case TokenNameEQUAL  :
        //  case TokenNameLBRACKET  :
        //  case TokenNameDOT :
        //  case TokenNameERROR :
        //  case TokenNameEOF  :
        //  case TokenNamecase  :
        //  case TokenNamecatch  :
        //  case TokenNameelse  :
        //  case TokenNameextends  :
        //  case TokenNamefinally  :
        //  case TokenNameimplements  :
        //  case TokenNamethrows  :
        //  case TokenNameinstanceof  :
        //  case TokenNameEQUAL_EQUAL  :
        //  case TokenNameLESS_EQUAL  :
        //  case TokenNameGREATER_EQUAL  :
        //  case TokenNameNOT_EQUAL  :
        //  case TokenNameLEFT_SHIFT  :
        //  case TokenNameRIGHT_SHIFT  :
        //  case TokenNameUNSIGNED_RIGHT_SHIFT :
        //  case TokenNamePLUS_EQUAL  :
        //  case TokenNameMINUS_EQUAL  :
        //  case TokenNameMULTIPLY_EQUAL  :
        //  case TokenNameDIVIDE_EQUAL  :
        //  case TokenNameAND_EQUAL  :
        //  case TokenNameOR_EQUAL  :
        //  case TokenNameXOR_EQUAL  :
        //  case TokenNameREMAINDER_EQUAL  :
        //  case TokenNameLEFT_SHIFT_EQUAL  :
        //  case TokenNameRIGHT_SHIFT_EQUAL  :
        //  case TokenNameUNSIGNED_RIGHT_SHIFT_EQUAL  :
        //  case TokenNameOR_OR  :
        //  case TokenNameAND_AND  :
        //  case TokenNameREMAINDER :
        //  case TokenNameXOR  :
        //  case TokenNameAND  :
        //  case TokenNameMULTIPLY :
        //  case TokenNameOR  :
        //  case TokenNameDIVIDE :
        //  case TokenNameGREATER  :
    }
}