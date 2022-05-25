public static void addUncaughtExceptionProposals(IInvocationContext context, IProblemLocation problem, Collection<ICommandAccess> proposals) throws CoreException {
    ICompilationUnit cu= context.getCompilationUnit();
    CompilationUnit astRoot= context.getASTRoot();
    ASTNode selectedNode= problem.getCoveringNode(astRoot);
    if (selectedNode == null) {
        return;
    }
    while (selectedNode != null && !(selectedNode instanceof Statement) && !(selectedNode instanceof VariableDeclarationExpression)) {
        selectedNode= selectedNode.getParent();
    }
    if (selectedNode == null) {
        return;
    }
    int offset= selectedNode.getStartPosition();
    int length= selectedNode.getLength();
    int selectionEnd= context.getSelectionOffset() + context.getSelectionLength();
    if (selectionEnd > offset + length) {
        // extend the selection if more than one statement is selected (bug 72149)
        length= selectionEnd - offset;
    }
    //Surround with proposals
    SurroundWithTryCatchRefactoring refactoring= SurroundWithTryCatchRefactoring.create(cu, offset, length);
    if (refactoring == null)
        return;
    refactoring.setLeaveDirty(true);
    if (refactoring.checkActivationBasics(astRoot).isOK()) {
        String label= CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_trycatch_description;
        Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
        RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, cu, refactoring, IProposalRelevance.SURROUND_WITH_TRY_CATCH, image);
        proposal.setLinkedProposalModel(refactoring.getLinkedProposalModel());
        proposals.add(proposal);
    }
    if (JavaModelUtil.is17OrHigher(cu.getJavaProject())) {
        refactoring= SurroundWithTryCatchRefactoring.create(cu, offset, length, true);
        if (refactoring == null)
            return;
        refactoring.setLeaveDirty(true);
        if (refactoring.checkActivationBasics(astRoot).isOK()) {
            String label= CorrectionMessages.LocalCorrectionsSubProcessor_surroundwith_trymulticatch_description;
            Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
            RefactoringCorrectionProposal proposal= new RefactoringCorrectionProposal(label, cu, refactoring, IProposalRelevance.SURROUND_WITH_TRY_MULTICATCH, image);
            proposal.setLinkedProposalModel(refactoring.getLinkedProposalModel());
            proposals.add(proposal);
        }
    }
    //Catch exception
    BodyDeclaration decl= ASTResolving.findParentBodyDeclaration(selectedNode);
    if (decl == null) {
        return;
    }
    ITypeBinding[] uncaughtExceptions= ExceptionAnalyzer.perform(decl, Selection.createFromStartLength(offset, length));
    if (uncaughtExceptions.length == 0) {
        return;
    }
    TryStatement surroundingTry= ASTResolving.findParentTryStatement(selectedNode);
    AST ast= astRoot.getAST();
    if (surroundingTry != null && (ASTNodes.isParent(selectedNode, surroundingTry.getBody()) || selectedNode.getLocationInParent() == TryStatement.RESOURCES_PROPERTY)) {
        {
            ASTRewrite rewrite= ASTRewrite.create(surroundingTry.getAST());
            String label= CorrectionMessages.LocalCorrectionsSubProcessor_addadditionalcatch_description;
            Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
            LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.ADD_ADDITIONAL_CATCH, image);
            ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
            ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(decl, imports);
            CodeScopeBuilder.Scope scope= CodeScopeBuilder.perform(decl, Selection.createFromStartLength(offset, length)).
                                          findScope(offset, length);
            scope.setCursor(offset);
            ListRewrite clausesRewrite= rewrite.getListRewrite(surroundingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
            for (int i= 0; i < uncaughtExceptions.length; i++) {
                ITypeBinding excBinding= uncaughtExceptions[i];
                String varName= StubUtility.getExceptionVariableName(cu.getJavaProject());
                String name= scope.createName(varName, false);
                SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
                var.setName(ast.newSimpleName(name));
                var.setType(imports.addImport(excBinding, ast, importRewriteContext));
                CatchClause newClause= ast.newCatchClause();
                newClause.setException(var);
                String catchBody= StubUtility.getCatchBodyContent(cu, excBinding.getName(), name, selectedNode, String.valueOf('\n'));
                if (catchBody != null) {
                    ASTNode node= rewrite.createStringPlaceholder(catchBody, ASTNode.RETURN_STATEMENT);
                    newClause.getBody().statements().add(node);
                }
                clausesRewrite.insertLast(newClause, null);
                String typeKey= "type" + i; //$NON-NLS-1$
                String nameKey= "name" + i; //$NON-NLS-1$
                proposal.addLinkedPosition(rewrite.track(var.getType()), false, typeKey);
                proposal.addLinkedPosition(rewrite.track(var.getName()), false, nameKey);
                addExceptionTypeLinkProposals(proposal, excBinding, typeKey);
            }
            proposals.add(proposal);
        }
        if (JavaModelUtil.is17OrHigher(cu.getJavaProject())) {
            List<CatchClause> catchClauses= surroundingTry.catchClauses();
            if (catchClauses != null && catchClauses.size() == 1) {
                String label= uncaughtExceptions.length > 1
                              ? CorrectionMessages.LocalCorrectionsSubProcessor_addexceptionstoexistingcatch_description
                              : CorrectionMessages.LocalCorrectionsSubProcessor_addexceptiontoexistingcatch_description;
                Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
                ASTRewrite rewrite= ASTRewrite.create(ast);
                LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.ADD_EXCEPTIONS_TO_EXISTING_CATCH, image);
                ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
                ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(decl, imports);
                CatchClause catchClause= catchClauses.get(0);
                Type type= catchClause.getException().getType();
                if (type instanceof UnionType) {
                    UnionType unionType= (UnionType) type;
                    ListRewrite listRewrite= rewrite.getListRewrite(unionType, UnionType.TYPES_PROPERTY);
                    for (int i= 0; i < uncaughtExceptions.length; i++) {
                        ITypeBinding excBinding= uncaughtExceptions[i];
                        Type type2= imports.addImport(excBinding, ast, importRewriteContext);
                        listRewrite.insertLast(type2, null);
                        String typeKey= "type" + i; //$NON-NLS-1$
                        proposal.addLinkedPosition(rewrite.track(type2), false, typeKey);
                        addExceptionTypeLinkProposals(proposal, excBinding, typeKey);
                    }
                } else {
                    UnionType newUnionType= ast.newUnionType();
                    List<Type> types= newUnionType.types();
                    types.add((Type) rewrite.createCopyTarget(type));
                    for (int i= 0; i < uncaughtExceptions.length; i++) {
                        ITypeBinding excBinding= uncaughtExceptions[i];
                        Type type2= imports.addImport(excBinding, ast, importRewriteContext);
                        types.add(type2);
                        String typeKey= "type" + i; //$NON-NLS-1$
                        proposal.addLinkedPosition(rewrite.track(type2), false, typeKey);
                        addExceptionTypeLinkProposals(proposal, excBinding, typeKey);
                    }
                    rewrite.replace(type, newUnionType, null);
                }
                proposals.add(proposal);
            } else if (catchClauses != null && catchClauses.size() == 0 && uncaughtExceptions.length > 1) {
                String label= CorrectionMessages.LocalCorrectionsSubProcessor_addadditionalmulticatch_description;
                Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
                ASTRewrite rewrite= ASTRewrite.create(ast);
                LinkedCorrectionProposal proposal= new LinkedCorrectionProposal(label, cu, rewrite, IProposalRelevance.ADD_ADDITIONAL_MULTI_CATCH, image);
                ImportRewrite imports= proposal.createImportRewrite(context.getASTRoot());
                ImportRewriteContext importRewriteContext= new ContextSensitiveImportRewriteContext(decl, imports);
                CodeScopeBuilder.Scope scope= CodeScopeBuilder.perform(decl, Selection.createFromStartLength(offset, length)).
                                              findScope(offset, length);
                scope.setCursor(offset);
                CatchClause newCatchClause= ast.newCatchClause();
                String varName= StubUtility.getExceptionVariableName(cu.getJavaProject());
                String name= scope.createName(varName, false);
                SingleVariableDeclaration var= ast.newSingleVariableDeclaration();
                var.setName(ast.newSimpleName(name));
                UnionType newUnionType= ast.newUnionType();
                List<Type> types= newUnionType.types();
                for (int i= 0; i < uncaughtExceptions.length; i++) {
                    ITypeBinding excBinding= uncaughtExceptions[i];
                    Type type2= imports.addImport(excBinding, ast, importRewriteContext);
                    types.add(type2);
                    String typeKey= "type" + i; //$NON-NLS-1$
                    proposal.addLinkedPosition(rewrite.track(type2), false, typeKey);
                    addExceptionTypeLinkProposals(proposal, excBinding, typeKey);
                }
                String nameKey= "name"; //$NON-NLS-1$
                proposal.addLinkedPosition(rewrite.track(var.getName()), false, nameKey);
                var.setType(newUnionType);
                newCatchClause.setException(var);
                String catchBody= StubUtility.getCatchBodyContent(cu, "Exception", name, selectedNode, String.valueOf('\n')); //$NON-NLS-1$
                if (catchBody != null) {
                    ASTNode node= rewrite.createStringPlaceholder(catchBody, ASTNode.RETURN_STATEMENT);
                    newCatchClause.getBody().statements().add(node);
                }
                ListRewrite listRewrite= rewrite.getListRewrite(surroundingTry, TryStatement.CATCH_CLAUSES_PROPERTY);
                listRewrite.insertFirst(newCatchClause, null);
                proposals.add(proposal);
            }
        }
    }
    //Add throws declaration
    if (decl instanceof MethodDeclaration) {
        MethodDeclaration methodDecl= (MethodDeclaration) decl;
        IMethodBinding binding= methodDecl.resolveBinding();
        boolean isApplicable= (binding != null);
        if (isApplicable) {
            IMethodBinding overriddenMethod= Bindings.findOverriddenMethod(binding, true);
            if (overriddenMethod != null ) {
                isApplicable= overriddenMethod.getDeclaringClass().isFromSource();
                if (!isApplicable) { // bug 349051
                    ITypeBinding[] exceptionTypes= overriddenMethod.getExceptionTypes();
                    ArrayList<ITypeBinding> unhandledExceptions= new ArrayList<ITypeBinding>(uncaughtExceptions.length);
                    for (int i= 0; i < uncaughtExceptions.length; i++) {
                        ITypeBinding curr= uncaughtExceptions[i];
                        if (isSubtype(curr, exceptionTypes)) {
                            unhandledExceptions.add(curr);
                        }
                    }
                    uncaughtExceptions= unhandledExceptions.toArray(new ITypeBinding[unhandledExceptions.size()]);
                    isApplicable|= uncaughtExceptions.length > 0;
                }
            }
        }
        if (isApplicable) {
            ITypeBinding[] methodExceptions= binding.getExceptionTypes();
            ArrayList<ITypeBinding> unhandledExceptions= new ArrayList<ITypeBinding>(uncaughtExceptions.length);
            for (int i= 0; i < uncaughtExceptions.length; i++) {
                ITypeBinding curr= uncaughtExceptions[i];
                if (!isSubtype(curr, methodExceptions)) {
                    unhandledExceptions.add(curr);
                }
            }
            uncaughtExceptions= unhandledExceptions.toArray(new ITypeBinding[unhandledExceptions.size()]);
            List<Name> exceptions= methodDecl.thrownExceptions();
            int nExistingExceptions= exceptions.size();
            ChangeDescription[] desc= new ChangeDescription[nExistingExceptions + uncaughtExceptions.length];
            for (int i= 0; i < exceptions.size(); i++) {
                Name elem= exceptions.get(i);
                if (isSubtype(elem.resolveTypeBinding(), uncaughtExceptions)) {
                    desc[i]= new RemoveDescription();
                }
            }
            for (int i = 0; i < uncaughtExceptions.length; i++) {
                desc[i + nExistingExceptions]= new InsertDescription(uncaughtExceptions[i], ""); //$NON-NLS-1$
            }
            String label= CorrectionMessages.LocalCorrectionsSubProcessor_addthrows_description;
            Image image= JavaPluginImages.get(JavaPluginImages.IMG_OBJS_EXCEPTION);
            ChangeMethodSignatureProposal proposal= new ChangeMethodSignatureProposal(label, cu, astRoot, binding, null, desc, IProposalRelevance.ADD_THROWS_DECLARATION, image);
            for (int i= 0; i < uncaughtExceptions.length; i++) {
                addExceptionTypeLinkProposals(proposal, uncaughtExceptions[i], proposal.getExceptionTypeGroupId(i + nExistingExceptions));
            }
            proposal.setCommandId(ADD_EXCEPTION_TO_THROWS_ID);
            proposals.add(proposal);
        }
    }
}