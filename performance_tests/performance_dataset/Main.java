protected void handleWarningToken(String token, boolean isEnabling, boolean useEnableJavadoc) throws InvalidInputException {
    if (token.equals("constructorName")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportMethodWithConstructorName,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("pkgDefaultMethod") || token.equals("packageDefaultMethod")/*backward compatible*/ ) { //$NON-NLS-1$ //$NON-NLS-2$
        this.options.put(
            CompilerOptions.OPTION_ReportOverridingPackageDefaultMethod,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("maskedCatchBlock") || token.equals("maskedCatchBlocks")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
        this.options.put(
            CompilerOptions.OPTION_ReportHiddenCatchBlock,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("deprecation")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportDeprecation,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode,
            CompilerOptions.DISABLED);
        this.options.put(
            CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod,
            CompilerOptions.DISABLED);
    } else if (token.equals("allDeprecation")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportDeprecation,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportDeprecationInDeprecatedCode,
            isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
        this.options.put(
            CompilerOptions.OPTION_ReportDeprecationWhenOverridingDeprecatedMethod,
            isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
    } else if (token.equals("unusedLocal") || token.equals("unusedLocals")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedLocal,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("unusedArgument") || token.equals("unusedArguments")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedParameter,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("unusedImport") || token.equals("unusedImports")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedImport,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("unusedPrivate")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedPrivateMember,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("unusedLabel")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedLabel,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("localHiding")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportLocalVariableHiding,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("fieldHiding")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportFieldHiding,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("specialParamHiding")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportSpecialParameterHidingField,
            isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
    } else if (token.equals("conditionAssign")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportPossibleAccidentalBooleanAssignment,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("syntheticAccess") //$NON-NLS-1$
               || token.equals("synthetic-access")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportSyntheticAccessEmulation,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("nls")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportNonExternalizedStringLiteral,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("staticReceiver")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportNonStaticAccessToStatic,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("indirectStatic")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportIndirectStaticAccess,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("noEffectAssign")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportNoEffectAssignment,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("intfNonInherited") || token.equals("interfaceNonInherited")/*backward compatible*/) { //$NON-NLS-1$ //$NON-NLS-2$
        this.options.put(
            CompilerOptions.OPTION_ReportIncompatibleNonInheritedInterfaceMethod,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("charConcat") || token.equals("noImplicitStringConversion")/*backward compatible*/) {//$NON-NLS-1$ //$NON-NLS-2$
        this.options.put(
            CompilerOptions.OPTION_ReportNoImplicitStringConversion,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("semicolon")) {//$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportEmptyStatement,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("serial")) {//$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportMissingSerialVersion,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("emptyBlock")) {//$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportUndocumentedEmptyBlock,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("uselessTypeCheck")) {//$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportUnnecessaryTypeCheck,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("unchecked") || token.equals("unsafe")) {//$NON-NLS-1$ //$NON-NLS-2$
        this.options.put(
            CompilerOptions.OPTION_ReportUncheckedTypeOperation,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("raw")) {//$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportRawTypeReference,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("finalBound")) {//$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportFinalParameterBound,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("suppress")) {//$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_SuppressWarnings,
            isEnabling ? CompilerOptions.ENABLED : CompilerOptions.DISABLED);
    } else if (token.equals("warningToken")) {//$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportUnhandledWarningToken,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("unnecessaryElse")) {//$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportUnnecessaryElse,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("javadoc")) {//$NON-NLS-1$
        if (!useEnableJavadoc) {
            this.options.put(
                CompilerOptions.OPTION_DocCommentSupport,
                isEnabling ? CompilerOptions.ENABLED: CompilerOptions.DISABLED);
        }
        // if disabling then it's not necessary to set other javadoc options
        if (isEnabling) {
            this.options.put(
                CompilerOptions.OPTION_ReportInvalidJavadoc,
                CompilerOptions.WARNING);
            this.options.put(
                CompilerOptions.OPTION_ReportInvalidJavadocTags,
                CompilerOptions.ENABLED);
            this.options.put(
                CompilerOptions.OPTION_ReportInvalidJavadocTagsDeprecatedRef,
                CompilerOptions.DISABLED);
            this.options.put(
                CompilerOptions.OPTION_ReportInvalidJavadocTagsNotVisibleRef,
                CompilerOptions.DISABLED);
            this.options.put(
                CompilerOptions.OPTION_ReportInvalidJavadocTagsVisibility,
                CompilerOptions.PRIVATE);
            this.options.put(
                CompilerOptions.OPTION_ReportMissingJavadocTags,
                CompilerOptions.WARNING);
            this.options.put(
                CompilerOptions.OPTION_ReportMissingJavadocTagsVisibility,
                CompilerOptions.PRIVATE);
        }
    } else if (token.equals("allJavadoc")) { //$NON-NLS-1$
        if (!useEnableJavadoc) {
            this.options.put(
                CompilerOptions.OPTION_DocCommentSupport,
                isEnabling ? CompilerOptions.ENABLED: CompilerOptions.DISABLED);
        }
        // if disabling then it's not necessary to set other javadoc options
        if (isEnabling) {
            this.options.put(
                CompilerOptions.OPTION_ReportInvalidJavadoc,
                CompilerOptions.WARNING);
            this.options.put(
                CompilerOptions.OPTION_ReportInvalidJavadocTags,
                CompilerOptions.ENABLED);
            this.options.put(
                CompilerOptions.OPTION_ReportInvalidJavadocTagsVisibility,
                CompilerOptions.PRIVATE);
            this.options.put(
                CompilerOptions.OPTION_ReportMissingJavadocTags,
                CompilerOptions.WARNING);
            this.options.put(
                CompilerOptions.OPTION_ReportMissingJavadocTagsVisibility,
                CompilerOptions.PRIVATE);
            this.options.put(
                CompilerOptions.OPTION_ReportMissingJavadocComments,
                CompilerOptions.WARNING);
        }
    } else if (token.startsWith("tasks")) { //$NON-NLS-1$
        String taskTags = Util.EMPTY_STRING;
        int start = token.indexOf('(');
        int end = token.indexOf(')');
        if (start >= 0 && end >= 0 && start < end) {
            taskTags = token.substring(start+1, end).trim();
            taskTags = taskTags.replace('|',',');
        }
        if (taskTags.length() == 0) {
            throw new InvalidInputException(this.bind("configure.invalidTaskTag", token)); //$NON-NLS-1$
        }
        this.options.put(
            CompilerOptions.OPTION_TaskTags,
            isEnabling ? taskTags : Util.EMPTY_STRING);
    } else if (token.equals("assertIdentifier")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportAssertIdentifier,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("enumIdentifier")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportEnumIdentifier,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("finally")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportFinallyBlockNotCompletingNormally,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("unusedThrown")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedDeclaredThrownException,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("unqualifiedField") //$NON-NLS-1$
               || token.equals("unqualified-field-access")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportUnqualifiedFieldAccess,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("typeHiding")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportTypeParameterHiding,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("varargsCast")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportVarargsArgumentNeedCast,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("null")) { //$NON-NLS-1$
        if (isEnabling) {
            this.options.put(CompilerOptions.OPTION_ReportNullReference,
                             CompilerOptions.WARNING);
            this.options.put(CompilerOptions.OPTION_ReportPotentialNullReference,
                             CompilerOptions.WARNING);
            this.options.put(CompilerOptions.OPTION_ReportRedundantNullCheck,
                             CompilerOptions.WARNING);
        } else {
            this.options.put(CompilerOptions.OPTION_ReportNullReference,
                             CompilerOptions.IGNORE);
            this.options.put(CompilerOptions.OPTION_ReportPotentialNullReference,
                             CompilerOptions.IGNORE);
            this.options.put(CompilerOptions.OPTION_ReportRedundantNullCheck,
                             CompilerOptions.IGNORE);
        }
    } else if (token.equals("nullDereference")) { //$NON-NLS-1$
        if (isEnabling) {
            this.options.put(CompilerOptions.OPTION_ReportNullReference,
                             CompilerOptions.WARNING);
        } else {
            this.options.put(CompilerOptions.OPTION_ReportNullReference,
                             CompilerOptions.IGNORE);
            this.options.put(CompilerOptions.OPTION_ReportPotentialNullReference,
                             CompilerOptions.IGNORE);
            this.options.put(CompilerOptions.OPTION_ReportRedundantNullCheck,
                             CompilerOptions.IGNORE);
        }
    } else if (token.equals("boxing")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportAutoboxing,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("over-ann")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportMissingOverrideAnnotation,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("dep-ann")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportMissingDeprecatedAnnotation,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("intfAnnotation")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportAnnotationSuperInterface,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("enumSwitch") //$NON-NLS-1$
               || token.equals("incomplete-switch")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportIncompleteEnumSwitch,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("hiding")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportHiddenCatchBlock,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportLocalVariableHiding,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportFieldHiding,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportTypeParameterHiding,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("static-access")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportNonStaticAccessToStatic,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportIndirectStaticAccess,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("unused")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedLocal,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedParameter,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedImport,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedPrivateMember,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedDeclaredThrownException,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
        this.options.put(
            CompilerOptions.OPTION_ReportUnusedLabel,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("paramAssign")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportParameterAssignment,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("discouraged")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportDiscouragedReference,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("forbidden")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportForbiddenReference,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("fallthrough")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportFallthroughCase,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else if (token.equals("super")) { //$NON-NLS-1$
        this.options.put(
            CompilerOptions.OPTION_ReportOverridingMethodWithoutSuperInvocation,
            isEnabling ? CompilerOptions.WARNING : CompilerOptions.IGNORE);
    } else {
        throw new InvalidInputException(this.bind("configure.invalidWarning", token)); //$NON-NLS-1$
    }
}