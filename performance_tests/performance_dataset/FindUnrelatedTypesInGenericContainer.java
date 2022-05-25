private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
    if (isSynthetic(method) || !prescreen(classContext, method))
        return;
    XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
    if (xmethod.isSynthetic()) return;
    BugAccumulator accumulator = new BugAccumulator(bugReporter);
    CFG cfg = classContext.getCFG(method);
    TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
    ValueNumberDataflow vnDataflow = classContext.getValueNumberDataflow(method);
    ConstantPoolGen cpg = classContext.getConstantPoolGen();
    MethodGen methodGen = classContext.getMethodGen(method);
    if (methodGen == null)
        return;
    String fullMethodName = methodGen.getClassName() + "." + methodGen.getName();
    String sourceFile = classContext.getJavaClass().getSourceFileName();
    if (DEBUG) {
        System.out.println("Checking " + fullMethodName);
    }
    // Process each instruction
    for (Iterator<Location> iter = cfg.locationIterator(); iter.hasNext();) {
        Location location = iter.next();
        InstructionHandle handle = location.getHandle();
        Instruction ins = handle.getInstruction();
        // Only consider invoke instructions
        if (!(ins instanceof InvokeInstruction))
            continue;
        InvokeInstruction inv = (InvokeInstruction) ins;
        XMethod invokedMethod = XFactory.createXMethod(inv, cpg);
        String invokedMethodName = invokedMethod.getName();
        for (ClassDescriptor interfaceOfInterest : nameToInterfaceMap.get(invokedMethodName)) {
            if (DEBUG) System.out.println("Checking call to " + interfaceOfInterest + " : " + invokedMethod);
            String argSignature = invokedMethod.getSignature();
            argSignature = argSignature.substring(0, argSignature.indexOf(')') + 1);
            int pos = 0;
            boolean allMethod = false;
            if (!argSignature.equals("(Ljava/lang/Object;)")) {
                if (invokedMethodName.equals("removeAll") || invokedMethodName.equals("containsAll")
                        || invokedMethodName.equals("retainAll")) {
                    if (!invokedMethod.getSignature().equals("(Ljava/util/Collection;)Z")) continue;
                    allMethod = true;
                } else if (invokedMethodName.endsWith("ndexOf")
                           && invokedMethod.getClassName().equals("java.util.Vector") &&
                           argSignature.equals("(Ljava/lang/Object;I)"))
                    pos = 1;
                else continue;
            }
            Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();
            try {
                if (!subtypes2.isSubtype(invokedMethod.getClassDescriptor(), interfaceOfInterest))
                    continue;
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
                continue;
            }
            // OK, we've fold a method call of interest
            int typeArgument = nameToTypeArgumentIndex.get(invokedMethodName);
            TypeFrame frame = typeDataflow.getFactAtLocation(location);
            if (!frame.isValid()) {
                // This basic block is probably dead
                continue;
            }
            Type operandType = frame.getStackValue(pos);
            if (operandType.equals(TopType.instance())) {
                // unreachable
                continue;
            }
            ValueNumberFrame vnFrame = vnDataflow.getFactAtLocation(location);
            int numArguments = frame.getNumArguments(inv, cpg);
            if (numArguments != 1+pos)
                continue;
            int expectedParameters = 1;
            if (interfaceOfInterest.getSimpleName().equals("Map"))
                expectedParameters = 2;
            // compare containers type parameters to corresponding arguments
            SignatureParser sigParser = new SignatureParser(inv.getSignature(cpg));
            ValueNumber objectVN = vnFrame.getInstance(ins, cpg);
            ValueNumber argVN = vnFrame.getArgument(inv, cpg, 0, sigParser);
            if (objectVN.equals(argVN)) {
                String bugPattern =  "DMI_COLLECTIONS_SHOULD_NOT_CONTAIN_THEMSELVES";
                int priority = HIGH_PRIORITY;
                if (invokedMethodName.equals("removeAll")) {
                    bugPattern = "DMI_USING_REMOVEALL_TO_CLEAR_COLLECTION";
                    priority = NORMAL_PRIORITY;
                } else if (invokedMethodName.endsWith("All")) {
                    bugPattern = "DMI_VACUOUS_SELF_COLLECTION_CALL";
                    priority = NORMAL_PRIORITY;
                }
                if (invokedMethodName.startsWith("contains")) {
                    InstructionHandle next = handle.getNext();
                    if (next != null) {
                        Instruction nextIns = next.getInstruction();
                        if (nextIns instanceof InvokeInstruction) {
                            XMethod nextMethod = XFactory.createXMethod((InvokeInstruction) nextIns, cpg);
                            if (nextMethod.getName().equals("assertTrue"))
                                continue;
                        }
                    }
                }
                accumulator.accumulateBug(new BugInstance(this,bugPattern, priority)
                                          .addClassAndMethod(methodGen,
                                                  sourceFile).addCalledMethod(
                                              methodGen, (InvokeInstruction) ins)
                                          .addOptionalAnnotation(ValueNumberSourceInfo.findAnnotationFromValueNumber(method,
                                                  location, objectVN, vnFrame, "INVOKED_ON")),
                                          SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                                                  sourceFile, handle));
            }
            // Only consider generic...
            Type objectType = frame.getInstance(inv, cpg);
            if (!(objectType instanceof GenericObjectType))
                continue;
            GenericObjectType operand = (GenericObjectType) objectType;
            // ... containers
            if (!operand.hasParameters())
                continue;
            ClassDescriptor operandClass = DescriptorFactory.getClassDescriptor(operand);
            if (!operandClass.getClassName().startsWith("java/util")) try {
                    XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, operandClass);
                    String sig = xclass.getSourceSignature();
                    if (sig != null && sig.indexOf("<L") > 0)
                        continue;
                } catch (CheckedAnalysisException e1) {
                    AnalysisContext.logError("Error checking for weird generic parameterization", e1);
                }
            if (operand.getNumParameters() != expectedParameters)
                continue;
            Type expectedType;
            if (typeArgument < 0) expectedType = operand;
            else expectedType = operand.getParameterAt(typeArgument);
            Type actualType = frame.getArgument(inv, cpg, 0, sigParser);
            IncompatibleTypes matchResult = compareTypes(expectedType, actualType, allMethod);
            boolean parmIsObject = expectedType.getSignature().equals("Ljava/lang/Object;");
            boolean selfOperation = !allMethod && operand.equals(actualType) && !parmIsObject;
            if (!allMethod && !parmIsObject && actualType instanceof GenericObjectType) {
                GenericObjectType p2 = (GenericObjectType) actualType;
                List<? extends ReferenceType> parameters = p2.getParameters();
                if (parameters != null && parameters.equals(operand.getParameters()))
                    selfOperation = true;
            }
            if (!selfOperation && matchResult == IncompatibleTypes.SEEMS_OK)
                continue;
            if (invokedMethodName.startsWith("contains") || invokedMethodName.equals("remove")) {
                InstructionHandle next = handle.getNext();
                if (next != null) {
                    Instruction nextIns = next.getInstruction();
                    if (nextIns instanceof InvokeInstruction) {
                        XMethod nextMethod = XFactory.createXMethod((InvokeInstruction) nextIns, cpg);
                        if (nextMethod.getName().equals("assertFalse"))
                            continue;
                    }
                }
            } else if (invokedMethodName.equals("get") || invokedMethodName.equals("remove")) {
                InstructionHandle next = handle.getNext();
                if (next != null) {
                    Instruction nextIns = next.getInstruction();
                    if (nextIns instanceof InvokeInstruction) {
                        XMethod nextMethod = XFactory.createXMethod((InvokeInstruction) nextIns, cpg);
                        if (nextMethod.getName().equals("assertNull"))
                            continue;
                    }
                }
            }
            boolean noisy = false;
            if (invokedMethodName.equals("get")) {
                UnconditionalValueDerefDataflow unconditionalValueDerefDataflow = classContext
                        .getUnconditionalValueDerefDataflow(method);
                UnconditionalValueDerefSet unconditionalDeref
                    = unconditionalValueDerefDataflow.getFactAtLocation(location);
                ValueNumberFrame vnAfter= vnDataflow.getFactAfterLocation(location);
                ValueNumber top = vnAfter.getTopValue();
                noisy = unconditionalDeref.getValueNumbersThatAreUnconditionallyDereferenced().contains(top);
            }
            // Prepare bug report
            SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                    sourceFile, handle);
            // Report a bug that mentions each of the failed arguments in
            // matches
            if (expectedType instanceof GenericObjectType)
                expectedType = ((GenericObjectType) expectedType).getUpperBound();
            int priority = matchResult.getPriority();
            if (TestCaseDetector.likelyTestCase(xmethod))
                priority = Math.max(priority,Priorities.NORMAL_PRIORITY);
            else if (selfOperation)
                priority = Priorities.HIGH_PRIORITY;
            ClassDescriptor expectedClassDescriptor = DescriptorFactory.createClassOrObjectDescriptorFromSignature(expectedType
                    .getSignature());
            ClassDescriptor actualClassDescriptor = DescriptorFactory.createClassOrObjectDescriptorFromSignature(actualType
                                                    .getSignature());
            ClassSummary classSummary = AnalysisContext.currentAnalysisContext().getClassSummary();
            Set<XMethod> targets = null;
            try {
                targets = Hierarchy2.resolveVirtualMethodCallTargets(actualClassDescriptor, "equals",
                          "(Ljava/lang/Object;)Z", false, false);
                boolean allOk = targets.size() > 0;
                for (XMethod m2 : targets)
                    if (!classSummary.mightBeEqualTo(m2.getClassDescriptor(), expectedClassDescriptor))
                        allOk = false;
                if (allOk)
                    priority += 2;
            } catch (ClassNotFoundException e) {
                AnalysisContext.reportMissingClass(e);
            }
            String bugPattern = "GC_UNRELATED_TYPES";
            if (matchResult == IncompatibleTypes.UNCHECKED) {
                boolean foundMatch = false;
                for (ClassDescriptor selfInterface : nameToInterfaceMap.get(methodGen.getName())) {
                    if (DEBUG) System.out.println("Checking call to " + interfaceOfInterest + " : " + invokedMethod);
                    String selfSignature = methodGen.getSignature();
                    argSignature = argSignature.substring(0, argSignature.indexOf(')') + 1);
                    try {
                        if (argSignature.equals("(Ljava/lang/Object;)")
                                && subtypes2.isSubtype(classContext.getClassDescriptor(), selfInterface)) {
                            foundMatch = true;
                            break;
                        }
                    } catch (ClassNotFoundException e) {
                        AnalysisContext.reportMissingClass(e);
                    }
                }
                if (foundMatch) continue;
                Instruction prevIns = handle.getPrev().getInstruction();
                if (prevIns instanceof InvokeInstruction) {
                    String returnValueSig = ((InvokeInstruction)prevIns).getSignature(cpg);
                    if (returnValueSig.endsWith(")Ljava/lang/Object;"))
                        continue;
                }
                bugPattern = "GC_UNCHECKED_TYPE_IN_GENERIC_CALL";
            }
            BugInstance bug = new BugInstance(this, bugPattern, priority).addClassAndMethod(methodGen,
                    sourceFile).addFoundAndExpectedType(actualType, expectedType).addCalledMethod(
                methodGen, (InvokeInstruction) ins)
            .addOptionalAnnotation(ValueNumberSourceInfo.findAnnotationFromValueNumber(method,
                                   location, objectVN, vnFrame, "INVOKED_ON"))
            .addOptionalAnnotation(ValueNumberSourceInfo.findAnnotationFromValueNumber(method,
                                   location, argVN, vnFrame, "ARGUMENT"))
            .addEqualsMethodUsed(targets);
            if (noisy) {
                WarningPropertySet<WarningProperty> propertySet = new WarningPropertySet<WarningProperty>();
                propertySet.addProperty(GeneralWarningProperty.NOISY_BUG);
                propertySet.decorateBugInstance(bug);
            }
            accumulator.accumulateBug(bug, sourceLineAnnotation);
        }
    }
    accumulator.reportAccumulatedBugs();
}