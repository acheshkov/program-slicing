public void generateCode(BlockScope currentScope, CodeStream codeStream) {
    if ((this.bits & ASTNode.IsReachable) == 0) {
        return;
    }
    boolean isStackMapFrameCodeStream = codeStream instanceof StackMapFrameCodeStream;
    // in case the labels needs to be reinitialized
    // when the code generation is restarted in wide mode
    this.anyExceptionLabel = null;
    this.reusableJSRTargets = null;
    this.reusableJSRSequenceStartLabels = null;
    this.reusableJSRTargetsCount = 0;
    int pc = codeStream.position;
    int finallyMode = finallyMode();
    boolean requiresNaturalExit = false;
    // preparing exception labels
    int maxCatches = this.catchArguments == null ? 0 : this.catchArguments.length;
    ExceptionLabel[] exceptionLabels;
    if (maxCatches > 0) {
        exceptionLabels = new ExceptionLabel[maxCatches];
        for (int i = 0; i < maxCatches; i++) {
            ExceptionLabel exceptionLabel = new ExceptionLabel(codeStream, this.catchArguments[i].binding.type);
            exceptionLabel.placeStart();
            exceptionLabels[i] = exceptionLabel;
        }
    } else {
        exceptionLabels = null;
    }
    if (this.subRoutineStartLabel != null) {
        this.subRoutineStartLabel.initialize(codeStream);
        this.enterAnyExceptionHandler(codeStream);
    }
    // generate the try block
    try {
        this.declaredExceptionLabels = exceptionLabels;
        this.tryBlock.generateCode(this.scope, codeStream);
    } finally {
        this.declaredExceptionLabels = null;
    }
    boolean tryBlockHasSomeCode = codeStream.position != pc;
    // flag telling if some bytecodes were issued inside the try block
    // place end positions of user-defined exception labels
    if (tryBlockHasSomeCode) {
        // natural exit may require subroutine invocation (if finally != null)
        BranchLabel naturalExitLabel = new BranchLabel(codeStream);
        BranchLabel postCatchesFinallyLabel = null;
        for (int i = 0; i < maxCatches; i++) {
            exceptionLabels[i].placeEnd();
        }
        if ((this.bits & ASTNode.IsTryBlockExiting) == 0) {
            int position = codeStream.position;
            switch(finallyMode) {
            case FINALLY_SUBROUTINE :
            case FINALLY_INLINE :
                requiresNaturalExit = true;
                if (this.naturalExitMergeInitStateIndex != -1) {
                    codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
                    codeStream.addDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
                }
                codeStream.goto_(naturalExitLabel);
                break;
            case NO_FINALLY :
                if (this.naturalExitMergeInitStateIndex != -1) {
                    codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
                    codeStream.addDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
                }
                codeStream.goto_(naturalExitLabel);
                break;
            case FINALLY_DOES_NOT_COMPLETE :
                codeStream.goto_(this.subRoutineStartLabel);
                break;
            }
            codeStream.updateLastRecordedEndPC(this.tryBlock.scope, position);
            //goto is tagged as part of the try block
        }
        /* generate sequence of handler, all starting by storing the TOS (exception
        thrown) into their own catch variables, the one specified in the source
        that must denote the handled exception.
        */
        this.exitAnyExceptionHandler();
        if (this.catchArguments != null) {
            postCatchesFinallyLabel = new BranchLabel(codeStream);
            for (int i = 0; i < maxCatches; i++) {
                enterAnyExceptionHandler(codeStream);
                // May loose some local variable initializations : affecting the local variable attributes
                if (this.preTryInitStateIndex != -1) {
                    codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
                    codeStream.addDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
                }
                codeStream.pushOnStack(exceptionLabels[i].exceptionType);
                exceptionLabels[i].place();
                // optimizing the case where the exception variable is not actually used
                LocalVariableBinding catchVar;
                int varPC = codeStream.position;
                if ((catchVar = this.catchArguments[i].binding).resolvedPosition != -1) {
                    codeStream.store(catchVar, false);
                    catchVar.recordInitializationStartPC(codeStream.position);
                    codeStream.addVisibleLocalVariable(catchVar);
                } else {
                    codeStream.pop();
                }
                codeStream.recordPositionsFrom(varPC, this.catchArguments[i].sourceStart);
                // Keep track of the pcs at diverging point for computing the local attribute
                // since not passing the catchScope, the block generation will exitUserScope(catchScope)
                this.catchBlocks[i].generateCode(this.scope, codeStream);
                this.exitAnyExceptionHandler();
                if (!this.catchExits[i]) {
                    switch(finallyMode) {
                    case FINALLY_INLINE :
                        // inlined finally here can see all merged variables
                        if (isStackMapFrameCodeStream) {
                            ((StackMapFrameCodeStream) codeStream).pushStateIndex(this.naturalExitMergeInitStateIndex);
                        }
                        if (this.catchExitInitStateIndexes[i] != -1) {
                            codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.catchExitInitStateIndexes[i]);
                            codeStream.addDefinitelyAssignedVariables(currentScope, this.catchExitInitStateIndexes[i]);
                        }
                        // entire sequence for finally is associated to finally block
                        this.finallyBlock.generateCode(this.scope, codeStream);
                        codeStream.goto_(postCatchesFinallyLabel);
                        if (isStackMapFrameCodeStream) {
                            ((StackMapFrameCodeStream) codeStream).popStateIndex();
                        }
                        break;
                    case FINALLY_SUBROUTINE :
                        requiresNaturalExit = true;
                    // fall through
                    case NO_FINALLY :
                        if (this.naturalExitMergeInitStateIndex != -1) {
                            codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
                            codeStream.addDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
                        }
                        codeStream.goto_(naturalExitLabel);
                        break;
                    case FINALLY_DOES_NOT_COMPLETE :
                        codeStream.goto_(this.subRoutineStartLabel);
                        break;
                    }
                }
            }
        }
        // extra handler for trailing natural exit (will be fixed up later on when natural exit is generated below)
        ExceptionLabel naturalExitExceptionHandler = requiresNaturalExit && (finallyMode == FINALLY_SUBROUTINE)
                ? new ExceptionLabel(codeStream, null)
                : null;
        // addition of a special handler so as to ensure that any uncaught exception (or exception thrown
        // inside catch blocks) will run the finally block
        int finallySequenceStartPC = codeStream.position;
        if (this.subRoutineStartLabel != null) {
            codeStream.pushOnStack(this.scope.getJavaLangThrowable());
            if (this.preTryInitStateIndex != -1) {
                // reset initialization state, as for a normal catch block
                codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
                codeStream.addDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
            }
            this.placeAllAnyExceptionHandler();
            if (naturalExitExceptionHandler != null) naturalExitExceptionHandler.place();
            switch(finallyMode) {
            case FINALLY_SUBROUTINE :
                // any exception handler
                codeStream.store(this.anyExceptionVariable, false);
                codeStream.jsr(this.subRoutineStartLabel);
                codeStream.recordPositionsFrom(finallySequenceStartPC, this.finallyBlock.sourceStart);
                int position = codeStream.position;
                codeStream.throwAnyException(this.anyExceptionVariable);
                codeStream.recordPositionsFrom(position, this.finallyBlock.sourceEnd);
                // subroutine
                this.subRoutineStartLabel.place();
                codeStream.pushOnStack(this.scope.getJavaLangThrowable());
                position = codeStream.position;
                codeStream.store(this.returnAddressVariable, false);
                codeStream.recordPositionsFrom(position, this.finallyBlock.sourceStart);
                this.finallyBlock.generateCode(this.scope, codeStream);
                position = codeStream.position;
                codeStream.ret(this.returnAddressVariable.resolvedPosition);
                codeStream.recordPositionsFrom(
                    position,
                    this.finallyBlock.sourceEnd);
                // the ret bytecode is part of the subroutine
                break;
            case FINALLY_INLINE :
                // any exception handler
                codeStream.store(this.anyExceptionVariable, false);
                codeStream.recordPositionsFrom(finallySequenceStartPC, this.finallyBlock.sourceStart);
                // subroutine
                this.finallyBlock.generateCode(currentScope, codeStream);
                position = codeStream.position;
                codeStream.throwAnyException(this.anyExceptionVariable);
                if (this.preTryInitStateIndex != -1) {
                    codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.preTryInitStateIndex);
                }
                this.subRoutineStartLabel.place();
                codeStream.recordPositionsFrom(position, this.finallyBlock.sourceEnd);
                break;
            case FINALLY_DOES_NOT_COMPLETE :
                // any exception handler
                codeStream.pop();
                this.subRoutineStartLabel.place();
                codeStream.recordPositionsFrom(finallySequenceStartPC, this.finallyBlock.sourceStart);
                // subroutine
                this.finallyBlock.generateCode(this.scope, codeStream);
                break;
            }
            // will naturally fall into subsequent code after subroutine invocation
            if (requiresNaturalExit) {
                switch(finallyMode) {
                case FINALLY_SUBROUTINE :
                    naturalExitLabel.place();
                    int position = codeStream.position;
                    naturalExitExceptionHandler.placeStart();
                    codeStream.jsr(this.subRoutineStartLabel);
                    naturalExitExceptionHandler.placeEnd();
                    codeStream.recordPositionsFrom(
                        position,
                        this.finallyBlock.sourceEnd);
                    break;
                case FINALLY_INLINE :
                    // inlined finally here can see all merged variables
                    if (isStackMapFrameCodeStream) {
                        ((StackMapFrameCodeStream) codeStream).pushStateIndex(this.naturalExitMergeInitStateIndex);
                    }
                    if (this.naturalExitMergeInitStateIndex != -1) {
                        codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
                        codeStream.addDefinitelyAssignedVariables(currentScope, this.naturalExitMergeInitStateIndex);
                    }
                    naturalExitLabel.place();
                    // entire sequence for finally is associated to finally block
                    this.finallyBlock.generateCode(this.scope, codeStream);
                    if (postCatchesFinallyLabel != null) {
                        position = codeStream.position;
                        // entire sequence for finally is associated to finally block
                        codeStream.goto_(postCatchesFinallyLabel);
                        codeStream.recordPositionsFrom(
                            position,
                            this.finallyBlock.sourceEnd);
                    }
                    if (isStackMapFrameCodeStream) {
                        ((StackMapFrameCodeStream) codeStream).popStateIndex();
                    }
                    break;
                case FINALLY_DOES_NOT_COMPLETE :
                    break;
                default :
                    naturalExitLabel.place();
                    break;
                }
            }
            if (postCatchesFinallyLabel != null) {
                postCatchesFinallyLabel.place();
            }
        } else {
            // no subroutine, simply position end label (natural exit == end)
            naturalExitLabel.place();
        }
    } else {
        // try block had no effect, only generate the body of the finally block if any
        if (this.subRoutineStartLabel != null) {
            this.finallyBlock.generateCode(this.scope, codeStream);
        }
    }
    // May loose some local variable initializations : affecting the local variable attributes
    if (this.mergedInitStateIndex != -1) {
        codeStream.removeNotDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
        codeStream.addDefinitelyAssignedVariables(currentScope, this.mergedInitStateIndex);
    }
    codeStream.recordPositionsFrom(pc, this.sourceStart);
}