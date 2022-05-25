public final boolean checkCastTypesCompatibility(Scope scope, TypeBinding castType, TypeBinding expressionType, Expression expression) {
    // see specifications 5.5
    // handle errors and process constant when needed
    // if either one of the type is null ==>
    // some error has been already reported some where ==>
    // we then do not report an obvious-cascade-error.
    if (castType == null || expressionType == null) return true;
    // identity conversion cannot be performed upfront, due to side-effects
    // like constant propagation
    boolean use15specifics = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_5;
    boolean use17specifics = scope.compilerOptions().sourceLevel >= ClassFileConstants.JDK1_7;
    if (castType.isBaseType()) {
        if (expressionType.isBaseType()) {
            if (expressionType == castType) {
                if (expression != null) {
                    this.constant = expression.constant; //use the same constant
                }
                tagAsUnnecessaryCast(scope, castType);
                return true;
            }
            boolean necessary = false;
            if (expressionType.isCompatibleWith(castType)
                    || (necessary = BaseTypeBinding.isNarrowing(castType.id, expressionType.id))) {
                if (expression != null) {
                    expression.implicitConversion = (castType.id << 4) + expressionType.id;
                    if (expression.constant != Constant.NotAConstant) {
                        this.constant = expression.constant.castTo(expression.implicitConversion);
                    }
                }
                if (!necessary) tagAsUnnecessaryCast(scope, castType);
                return true;
            }
        } else if (use17specifics && expressionType.id == TypeIds.T_JavaLangObject) {
            // cast from Object to base type allowed from 1.7, see JLS $5.5
            return true;
        } else if (use15specifics
                   && scope.environment().computeBoxingType(expressionType).isCompatibleWith(castType)) { // unboxing - only widening match is allowed
            tagAsUnnecessaryCast(scope, castType);
            return true;
        }
        return false;
    } else if (use15specifics
               && expressionType.isBaseType()
               && scope.environment().computeBoxingType(expressionType).isCompatibleWith(castType)) { // boxing - only widening match is allowed
        tagAsUnnecessaryCast(scope, castType);
        return true;
    }
    switch(expressionType.kind()) {
    case Binding.BASE_TYPE :
        //-----------cast to something which is NOT a base type--------------------------
        if (expressionType == TypeBinding.NULL) {
            tagAsUnnecessaryCast(scope, castType);
            return true; //null is compatible with every thing
        }
        return false;
    case Binding.ARRAY_TYPE :
        if (castType == expressionType) {
            tagAsUnnecessaryCast(scope, castType);
            return true; // identity conversion
        }
        switch (castType.kind()) {
        case Binding.ARRAY_TYPE :
            // ( ARRAY ) ARRAY
            TypeBinding castElementType = ((ArrayBinding) castType).elementsType();
            TypeBinding exprElementType = ((ArrayBinding) expressionType).elementsType();
            if (exprElementType.isBaseType() || castElementType.isBaseType()) {
                if (castElementType == exprElementType) {
                    tagAsNeedCheckCast();
                    return true;
                }
                return false;
            }
            // recurse on array type elements
            return checkCastTypesCompatibility(scope, castElementType, exprElementType, expression);
        case Binding.TYPE_PARAMETER :
            // ( TYPE_PARAMETER ) ARRAY
            TypeBinding match = expressionType.findSuperTypeOriginatingFrom(castType);
            if (match == null) {
                checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
            }
            // recurse on the type variable upper bound
            return checkCastTypesCompatibility(scope, ((TypeVariableBinding)castType).upperBound(), expressionType, expression);
        default:
            // ( CLASS/INTERFACE ) ARRAY
            switch (castType.id) {
            case T_JavaLangCloneable :
            case T_JavaIoSerializable :
                tagAsNeedCheckCast();
                return true;
            case T_JavaLangObject :
                tagAsUnnecessaryCast(scope, castType);
                return true;
            default :
                return false;
            }
        }
    case Binding.TYPE_PARAMETER :
        TypeBinding match = expressionType.findSuperTypeOriginatingFrom(castType);
        if (match != null) {
            return checkUnsafeCast(scope, castType, expressionType, match, false);
        }
        // recursively on the type variable upper bound
        return checkCastTypesCompatibility(scope, castType, ((TypeVariableBinding)expressionType).upperBound(), expression);
    case Binding.WILDCARD_TYPE :
    case Binding.INTERSECTION_TYPE :
        match = expressionType.findSuperTypeOriginatingFrom(castType);
        if (match != null) {
            return checkUnsafeCast(scope, castType, expressionType, match, false);
        }
        // recursively on the type variable upper bound
        return checkCastTypesCompatibility(scope, castType, ((WildcardBinding)expressionType).bound, expression);
    default:
        if (expressionType.isInterface()) {
            switch (castType.kind()) {
            case Binding.ARRAY_TYPE :
                // ( ARRAY ) INTERFACE
                switch (expressionType.id) {
                case T_JavaLangCloneable :
                case T_JavaIoSerializable :
                    tagAsNeedCheckCast();
                    return true;
                default :
                    return false;
                }
            case Binding.TYPE_PARAMETER :
                // ( INTERFACE ) TYPE_PARAMETER
                match = expressionType.findSuperTypeOriginatingFrom(castType);
                if (match == null) {
                    checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
                }
                // recurse on the type variable upper bound
                return checkCastTypesCompatibility(scope, ((TypeVariableBinding)castType).upperBound(), expressionType, expression);
            default :
                if (castType.isInterface()) {
                    // ( INTERFACE ) INTERFACE
                    ReferenceBinding interfaceType = (ReferenceBinding) expressionType;
                    match = interfaceType.findSuperTypeOriginatingFrom(castType);
                    if (match != null) {
                        return checkUnsafeCast(scope, castType, interfaceType, match, false);
                    }
                    tagAsNeedCheckCast();
                    match = castType.findSuperTypeOriginatingFrom(interfaceType);
                    if (match != null) {
                        return checkUnsafeCast(scope, castType, interfaceType, match, true);
                    }
                    if (use15specifics) {
                        checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
                        // ensure there is no collision between both interfaces: i.e. I1 extends List<String>, I2 extends List<Object>
                        if (scope.compilerOptions().complianceLevel < ClassFileConstants.JDK1_7) {
                            if (interfaceType.hasIncompatibleSuperType((ReferenceBinding) castType)) {
                                return false;
                            }
                        } else if (!castType.isRawType() && interfaceType.hasIncompatibleSuperType((ReferenceBinding) castType)) {
                            return false;
                        }
                    } else {
                        // pre1.5 semantics - no covariance allowed (even if 1.5 compliant, but 1.4 source)
                        // look at original methods rather than the parameterized variants at 1.4 to detect
                        // covariance. Otherwise when confronted with one raw type and one parameterized type,
                        // we could mistakenly detect covariance and scream foul. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=332744
                        MethodBinding[] castTypeMethods = getAllOriginalInheritedMethods((ReferenceBinding) castType);
                        MethodBinding[] expressionTypeMethods = getAllOriginalInheritedMethods((ReferenceBinding) expressionType);
                        int exprMethodsLength = expressionTypeMethods.length;
                        for (int i = 0, castMethodsLength = castTypeMethods.length; i < castMethodsLength; i++) {
                            for (int j = 0; j < exprMethodsLength; j++) {
                                if ((castTypeMethods[i].returnType != expressionTypeMethods[j].returnType)
                                        && (CharOperation.equals(castTypeMethods[i].selector, expressionTypeMethods[j].selector))
                                        && castTypeMethods[i].areParametersEqual(expressionTypeMethods[j])) {
                                    return false;
                                }
                            }
                        }
                    }
                    return true;
                } else {
                    // ( CLASS ) INTERFACE
                    if (castType.id == TypeIds.T_JavaLangObject) { // no runtime error
                        tagAsUnnecessaryCast(scope, castType);
                        return true;
                    }
                    // can only be a downcast
                    tagAsNeedCheckCast();
                    match = castType.findSuperTypeOriginatingFrom(expressionType);
                    if (match != null) {
                        return checkUnsafeCast(scope, castType, expressionType, match, true);
                    }
                    if (((ReferenceBinding) castType).isFinal()) {
                        // no subclass for castType, thus compile-time check is invalid
                        return false;
                    }
                    if (use15specifics) {
                        checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
                        // ensure there is no collision between both interfaces: i.e. I1 extends List<String>, I2 extends List<Object>
                        if (scope.compilerOptions().complianceLevel < ClassFileConstants.JDK1_7) {
                            if (((ReferenceBinding)castType).hasIncompatibleSuperType((ReferenceBinding) expressionType)) {
                                return false;
                            }
                        } else if (!castType.isRawType() && ((ReferenceBinding)castType).hasIncompatibleSuperType((ReferenceBinding) expressionType)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        } else {
            switch (castType.kind()) {
            case Binding.ARRAY_TYPE :
                // ( ARRAY ) CLASS
                if (expressionType.id == TypeIds.T_JavaLangObject) { // potential runtime error
                    if (use15specifics) checkUnsafeCast(scope, castType, expressionType, expressionType, true);
                    tagAsNeedCheckCast();
                    return true;
                }
                return false;
            case Binding.TYPE_PARAMETER :
                // ( TYPE_PARAMETER ) CLASS
                match = expressionType.findSuperTypeOriginatingFrom(castType);
                if (match == null) {
                    checkUnsafeCast(scope, castType, expressionType, null, true);
                }
                // recurse on the type variable upper bound
                return checkCastTypesCompatibility(scope, ((TypeVariableBinding)castType).upperBound(), expressionType, expression);
            default :
                if (castType.isInterface()) {
                    // ( INTERFACE ) CLASS
                    ReferenceBinding refExprType = (ReferenceBinding) expressionType;
                    match = refExprType.findSuperTypeOriginatingFrom(castType);
                    if (match != null) {
                        return checkUnsafeCast(scope, castType, expressionType, match, false);
                    }
                    // unless final a subclass may implement the interface ==> no check at compile time
                    if (refExprType.isFinal()) {
                        return false;
                    }
                    tagAsNeedCheckCast();
                    match = castType.findSuperTypeOriginatingFrom(expressionType);
                    if (match != null) {
                        return checkUnsafeCast(scope, castType, expressionType, match, true);
                    }
                    if (use15specifics) {
                        checkUnsafeCast(scope, castType, expressionType, null /*no match*/, true);
                        // ensure there is no collision between both interfaces: i.e. I1 extends List<String>, I2 extends List<Object>
                        if (scope.compilerOptions().complianceLevel < ClassFileConstants.JDK1_7) {
                            if (refExprType.hasIncompatibleSuperType((ReferenceBinding) castType)) {
                                return false;
                            }
                        } else if (!castType.isRawType() && refExprType.hasIncompatibleSuperType((ReferenceBinding) castType)) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    // ( CLASS ) CLASS
                    match = expressionType.findSuperTypeOriginatingFrom(castType);
                    if (match != null) {
                        if (expression != null && castType.id == TypeIds.T_JavaLangString) this.constant = expression.constant; // (String) cst is still a constant
                        return checkUnsafeCast(scope, castType, expressionType, match, false);
                    }
                    match = castType.findSuperTypeOriginatingFrom(expressionType);
                    if (match != null) {
                        tagAsNeedCheckCast();
                        return checkUnsafeCast(scope, castType, expressionType, match, true);
                    }
                    return false;
                }
            }
        }
    }
}