private void findLocalMethodsFromFavorites(
    char[] methodName,
    MethodBinding[] methods,
    Scope scope,
    ObjectVector methodsFound,
    ReferenceBinding receiverType,
    InvocationSite invocationSite,
    Scope invocationScope) {
    char[] typeName = CharOperation.concatWith(receiverType.compoundName, '.');
    int methodLength = methodName.length;
    next : for (int f = methods.length; --f >= 0;) {
        MethodBinding method = methods[f];
        if (method.isSynthetic()) continue next;
        if (method.isDefaultAbstract())	continue next;
        if (method.isConstructor()) continue next;
        if (this.options.checkDeprecation &&
                method.isViewedAsDeprecated() &&
                !scope.isDefinedInSameUnit(method.declaringClass))
            continue next;
        if (!method.isStatic()) continue next;
        if (this.options.checkVisibility
                && !method.canBeSeenBy(receiverType, invocationSite, scope)) continue next;
        if (methodLength > method.selector.length) continue next;
        if (!CharOperation.prefixEquals(methodName, method.selector, false /* ignore case */)
                && !(this.options.camelCaseMatch && CharOperation.camelCaseMatch(methodName, method.selector))) {
            continue next;
        }
        for (int i = methodsFound.size; --i >= 0;) {
            Object[] other = (Object[]) methodsFound.elementAt(i);
            MethodBinding otherMethod = (MethodBinding) other[0];
            if (method == otherMethod) continue next;
            if (CharOperation.equals(method.selector, otherMethod.selector, true)) {
                if (lookupEnvironment.methodVerifier().doesMethodOverride(otherMethod, method)) {
                    continue next;
                }
            }
        }
        boolean proposeStaticImport = !(this.compilerOptions.complianceLevel < ClassFileConstants.JDK1_5) &&
                                      this.options.suggestStaticImport;
        boolean isAlreadyImported = false;
        if (!proposeStaticImport) {
            if(!this.importCachesInitialized) {
                this.initializeImportCaches();
            }
            for (int j = 0; j < this.importCacheCount; j++) {
                char[][] importName = this.importsCache[j];
                if(CharOperation.equals(receiverType.sourceName, importName[0])) {
                    if (!CharOperation.equals(typeName, importName[1])) {
                        continue next;
                    } else {
                        isAlreadyImported = true;
                    }
                }
            }
        }
        methodsFound.add(new Object[] {method, receiverType});
        ReferenceBinding superTypeWithSameErasure = (ReferenceBinding)receiverType.findSuperTypeWithSameErasure(method.declaringClass);
        if (method.declaringClass != superTypeWithSameErasure) {
            MethodBinding[] otherMethods = superTypeWithSameErasure.getMethods(method.selector);
            for (int i = 0; i < otherMethods.length; i++) {
                if(otherMethods[i].original() == method.original()) {
                    method = otherMethods[i];
                }
            }
        }
        int length = method.parameters.length;
        char[][] parameterPackageNames = new char[length][];
        char[][] parameterTypeNames = new char[length][];
        for (int i = 0; i < length; i++) {
            TypeBinding type = method.original().parameters[i];
            parameterPackageNames[i] = type.qualifiedPackageName();
            parameterTypeNames[i] = type.qualifiedSourceName();
        }
        char[][] parameterNames = findMethodParameterNames(method,parameterTypeNames);
        char[] completion = CharOperation.NO_CHAR;
        int previousStartPosition = this.startPosition;
        if (this.source != null
                && this.source.length > this.endPosition
                && this.source[this.endPosition] == '(') {
            completion = method.selector;
        } else {
            completion = CharOperation.concat(method.selector, new char[] { '(', ')' });
        }
        int relevance = computeBaseRelevance();
        relevance += computeRelevanceForResolution();
        relevance += computeRelevanceForInterestingProposal();
        if (methodName != null) relevance += computeRelevanceForCaseMatching(methodName, method.selector);
        relevance += computeRelevanceForExpectingType(method.returnType);
        relevance += computeRelevanceForStatic(true, method.isStatic());
        relevance += computeRelevanceForQualification(true);
        relevance += computeRelevanceForRestrictions(IAccessRule.K_ACCESSIBLE);
        CompilationUnitDeclaration cu = this.unitScope.referenceContext;
        int importStart = cu.types[0].declarationSourceStart;
        int importEnd = importStart;
        this.noProposal = false;
        if (!proposeStaticImport) {
            if (isAlreadyImported) {
                if (!isIgnored(CompletionProposal.METHOD_REF)) {
                    completion = CharOperation.concat(receiverType.sourceName, completion, '.');
                    CompletionProposal proposal = this.createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
                    proposal.setDeclarationSignature(getSignature(method.declaringClass));
                    proposal.setSignature(getSignature(method));
                    MethodBinding original = method.original();
                    if(original != method) {
                        proposal.setOriginalSignature(getSignature(original));
                    }
                    proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
                    proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
                    proposal.setParameterPackageNames(parameterPackageNames);
                    proposal.setParameterTypeNames(parameterTypeNames);
                    proposal.setPackageName(method.returnType.qualifiedPackageName());
                    proposal.setTypeName(method.returnType.qualifiedSourceName());
                    proposal.setName(method.selector);
                    proposal.setCompletion(completion);
                    proposal.setFlags(method.modifiers);
                    proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
                    proposal.setRelevance(relevance);
                    if(parameterNames != null) proposal.setParameterNames(parameterNames);
                    this.requestor.accept(proposal);
                    if(DEBUG) {
                        this.printDebug(proposal);
                    }
                }
            } else if (!this.isIgnored(CompletionProposal.METHOD_REF, CompletionProposal.TYPE_IMPORT)) {
                completion = CharOperation.concat(receiverType.sourceName, completion, '.');
                CompletionProposal proposal = this.createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
                proposal.setDeclarationSignature(getSignature(method.declaringClass));
                proposal.setSignature(getSignature(method));
                MethodBinding original = method.original();
                if(original != method) {
                    proposal.setOriginalSignature(getSignature(original));
                }
                proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
                proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
                proposal.setParameterPackageNames(parameterPackageNames);
                proposal.setParameterTypeNames(parameterTypeNames);
                proposal.setPackageName(method.returnType.qualifiedPackageName());
                proposal.setTypeName(method.returnType.qualifiedSourceName());
                proposal.setName(method.selector);
                proposal.setCompletion(completion);
                proposal.setFlags(method.modifiers);
                proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
                proposal.setRelevance(relevance);
                if(parameterNames != null) proposal.setParameterNames(parameterNames);
                char[] typeImportCompletion = createImportCharArray(typeName, false, false);
                CompletionProposal typeImportProposal = this.createProposal(CompletionProposal.TYPE_IMPORT, this.actualCompletionPosition);
                typeImportProposal.nameLookup = this.nameEnvironment.nameLookup;
                typeImportProposal.completionEngine = this;
                char[] packageName = receiverType.qualifiedPackageName();
                typeImportProposal.setDeclarationSignature(packageName);
                typeImportProposal.setSignature(getSignature(receiverType));
                typeImportProposal.setPackageName(packageName);
                typeImportProposal.setTypeName(receiverType.qualifiedSourceName());
                typeImportProposal.setCompletion(typeImportCompletion);
                typeImportProposal.setFlags(receiverType.modifiers);
                typeImportProposal.setAdditionalFlags(CompletionFlags.Default);
                typeImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
                typeImportProposal.setRelevance(relevance);
                proposal.setRequiredProposals(new CompletionProposal[] {typeImportProposal});
                this.requestor.accept(proposal);
                if(DEBUG) {
                    this.printDebug(proposal);
                }
            }
        } else {
            if (!this.isIgnored(CompletionProposal.METHOD_REF, CompletionProposal.METHOD_IMPORT)) {
                CompletionProposal proposal = this.createProposal(CompletionProposal.METHOD_REF, this.actualCompletionPosition);
                proposal.setDeclarationSignature(getSignature(method.declaringClass));
                proposal.setSignature(getSignature(method));
                MethodBinding original = method.original();
                if(original != method) {
                    proposal.setOriginalSignature(getSignature(original));
                }
                proposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
                proposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
                proposal.setParameterPackageNames(parameterPackageNames);
                proposal.setParameterTypeNames(parameterTypeNames);
                proposal.setPackageName(method.returnType.qualifiedPackageName());
                proposal.setTypeName(method.returnType.qualifiedSourceName());
                proposal.setName(method.selector);
                proposal.setCompletion(completion);
                proposal.setFlags(method.modifiers);
                proposal.setReplaceRange(this.startPosition - this.offset, this.endPosition - this.offset);
                proposal.setRelevance(relevance);
                if(parameterNames != null) proposal.setParameterNames(parameterNames);
                char[] methodImportCompletion = createImportCharArray(CharOperation.concat(typeName, method.selector, '.'), true, false);
                CompletionProposal methodImportProposal = this.createProposal(CompletionProposal.METHOD_IMPORT, this.actualCompletionPosition);
                methodImportProposal.setDeclarationSignature(getSignature(method.declaringClass));
                methodImportProposal.setSignature(getSignature(method));
                if(original != method) {
                    proposal.setOriginalSignature(getSignature(original));
                }
                methodImportProposal.setDeclarationPackageName(method.declaringClass.qualifiedPackageName());
                methodImportProposal.setDeclarationTypeName(method.declaringClass.qualifiedSourceName());
                methodImportProposal.setParameterPackageNames(parameterPackageNames);
                methodImportProposal.setParameterTypeNames(parameterTypeNames);
                methodImportProposal.setPackageName(method.returnType.qualifiedPackageName());
                methodImportProposal.setTypeName(method.returnType.qualifiedSourceName());
                methodImportProposal.setName(method.selector);
                methodImportProposal.setCompletion(methodImportCompletion);
                methodImportProposal.setFlags(method.modifiers);
                methodImportProposal.setAdditionalFlags(CompletionFlags.StaticImport);
                methodImportProposal.setReplaceRange(importStart - this.offset, importEnd - this.offset);
                methodImportProposal.setRelevance(relevance);
                if(parameterNames != null) methodImportProposal.setParameterNames(parameterNames);
                proposal.setRequiredProposals(new CompletionProposal[] {methodImportProposal});
                this.requestor.accept(proposal);
                if(DEBUG) {
                    this.printDebug(proposal);
                }
            }
        }
        this.startPosition = previousStartPosition;
    }
}