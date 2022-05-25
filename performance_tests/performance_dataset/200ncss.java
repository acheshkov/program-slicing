public static Collection<TypeUsage> resolveTypeFromExpression (Model model, @NullAllowed Index jsIndex, List<String> exp, int offset, boolean includeAllPossible) {
    List<JsObject> localObjects = new ArrayList<JsObject>();
    List<JsObject> lastResolvedObjects = new ArrayList<JsObject>();
    List<TypeUsage> lastResolvedTypes = new ArrayList<TypeUsage>();
    for (int i = exp.size() - 1; i > -1; i--) {
        String kind = exp.get(i);
        String name = exp.get(--i);
        if (name.startsWith("@ano:")) {
            String[] parts = name.split(":");
            int anoOffset = Integer.parseInt(parts[1]);
            JsObject anonym = ModelUtils.findJsObject(model, anoOffset);
            lastResolvedObjects.add(anonym);
            continue;
        }
        if (ModelUtils.THIS.equals(name)) {
            JsObject thisObject = ModelUtils.findJsObject(model, offset);
            JsObject first = thisObject;
            while (thisObject != null && thisObject.getParent() != null && thisObject.getParent().getJSKind() != JsElement.Kind.FILE
                    && thisObject.getJSKind() != JsElement.Kind.CONSTRUCTOR
                    && thisObject.getJSKind() != JsElement.Kind.ANONYMOUS_OBJECT
                    && thisObject.getJSKind() != JsElement.Kind.OBJECT_LITERAL) {
                thisObject = thisObject.getParent();
            }
            if ((thisObject == null || thisObject.getParent() == null) && first != null) {
                thisObject = first;
            }
            if (thisObject != null) {
                name = thisObject.getName();
            }
        }
        if (i == (exp.size() - 2)) {
            JsObject localObject = null;
            int index = name.lastIndexOf('.');
            Collection<? extends TypeUsage> typeFromWith = getTypeFromWith(model, offset);
            if (!typeFromWith.isEmpty()) {
                String firstNamePart = index == -1 ? name : name.substring(0, index);
                String changedName = name;
                for (TypeUsage type : typeFromWith) {
                    String sType = type.getType();
                    localObject = ModelUtils.findJsObjectByName(model, sType);
                    if (localObject != null && localObject.getProperty(firstNamePart) != null) {
                        changedName = localObject.getFullyQualifiedName() + "." + name;
                    } else {
                        lastResolvedTypes.add(new TypeUsage(sType + "." + name, -1, true));
                    }
                }
                name = changedName;
            }
            if (index > -1) {
                localObject = ModelUtils.findJsObjectByName(model, name);
                if (localObject != null) {
                    localObjects.add(localObject);
                }
            } else {
                boolean canBeWindowsProp = true;
                for (JsObject object : model.getVariables(offset)) {
                    if (object.getName().equals(name)) {
                        localObjects.add(object);
                        localObject = object;
                        break;
                    }
                }
                if (localObject != null && localObject.getJSKind().isFunction() && i - 2 > -1) {
                    JsFunction localFunc = (JsFunction)localObject;
                    String paramName = exp.get(i - 2);
                    if (localFunc.getParameter(paramName) != null) {
                        canBeWindowsProp = false;
                    }
                }
                for (JsObject libGlobal : ModelExtender.getDefault().getExtendingGlobalObjects(model.getGlobalObject().getFileObject())) {
                    assert libGlobal != null;
                    for (JsObject object : libGlobal.getProperties().values()) {
                        if (object.getName().equals(name)) {
                            lastResolvedTypes.add(new TypeUsage(object.getName(), -1, true));
                            break;
                        }
                    }
                }
                if (jsIndex != null && canBeWindowsProp) {
                    Collection<TypeUsage> windowProperty = tryResolveWindowProperty(model, jsIndex, name);
                    if (windowProperty != null && !windowProperty.isEmpty()) {
                        lastResolvedTypes.addAll(windowProperty);
                    }
                }
            }
            if(localObject == null || (localObject.getJSKind() != JsElement.Kind.PARAMETER
                                       && (ModelUtils.isGlobal(localObject.getParent()) || localObject.getJSKind() != JsElement.Kind.VARIABLE))) {
                List<TypeUsage> fromAssignments = new ArrayList<TypeUsage>();
                if ("@pro".equals(kind) && jsIndex != null) {
                    resolveAssignments(model, jsIndex, name, -1,  fromAssignments);
                }
                lastResolvedTypes.addAll(fromAssignments);
                if (!typeFromWith.isEmpty()) {
                    for (TypeUsage typeUsage : typeFromWith) {
                        String sType = typeUsage.getType();
                        if (sType.startsWith("@exp;")) {
                            sType = sType.substring(5);
                            sType = sType.replace("@pro;", ".");
                        }
                        ModelUtils.resolveAssignments(model, jsIndex, sType, typeUsage.getOffset(), fromAssignments);
                        for (TypeUsage typeUsage1 : fromAssignments) {
                            String localFqn = localObject != null ? localObject.getFullyQualifiedName() : null;
                            if (localFqn != null  && name.startsWith(localFqn) && name.length() > localFqn.length() ) {
                                lastResolvedTypes.add(new TypeUsage(typeUsage1.getType() + kind + ";" + name.substring(localFqn.length() + 1), typeUsage.getOffset(), false));
                            } else {
                                if (!typeUsage1.getType().equals(name)) {
                                    lastResolvedTypes.add(new TypeUsage(typeUsage1.getType() + kind + ";" + name, typeUsage.getOffset(), false));
                                } else {
                                    lastResolvedTypes.add(typeUsage1);
                                }
                            }
                        }
                    }
                }
            }
            if(!localObjects.isEmpty()) {
                for(JsObject lObject : localObjects) {
                    if(lObject.getAssignmentForOffset(offset).isEmpty()) {
                        boolean addAsType = lObject.getJSKind() == JsElement.Kind.OBJECT_LITERAL;
                        if (lObject instanceof JsObjectReference) {
                            JsObject original = ((JsObjectReference)lObject).getOriginal();
                            if (original != null) {
                                name = original.getDeclarationName() != null ? original.getDeclarationName().getName() : original.getName();
                            }
                        }
                        if(addAsType) {
                            lastResolvedTypes.add(new TypeUsage(name, -1, true));
                        }
                    }
                    if ("@mtd".equals(kind)) {
                        if (lObject.getJSKind().isFunction()) {
                            lastResolvedTypes.addAll(((JsFunction) lObject).getReturnTypes());
                        }
                        int lastCallOffset = -1;
                        for (Occurrence occurrence : lObject.getOccurrences()) {
                            if (lastCallOffset < occurrence.getOffsetRange().getStart() && occurrence.getOffsetRange().getStart() <= offset) {
                                lastCallOffset = occurrence.getOffsetRange().getStart();
                            }
                        }
                        Collection<TypeUsage> returnTypesFromFrameworks = model.getReturnTypesFromFrameworks(lObject.getName(), lastCallOffset);
                        if (returnTypesFromFrameworks != null && !returnTypesFromFrameworks.isEmpty()) {
                            lastResolvedTypes.addAll(returnTypesFromFrameworks);
                        }
                        if (jsIndex != null) {
                            Collection<? extends IndexResult> findByFqn = jsIndex.findByFqn(name, Index.TERMS_BASIC_INFO);
                            for (Iterator<? extends IndexResult> iterator = findByFqn.iterator(); iterator.hasNext();) {
                                IndexedElement indexElement = IndexedElement.create(iterator.next());
                                if(indexElement instanceof IndexedElement.FunctionIndexedElement) {
                                    IndexedElement.FunctionIndexedElement iFunction = (IndexedElement.FunctionIndexedElement)indexElement;
                                    for (String type : iFunction.getReturnTypes()) {
                                        lastResolvedTypes.add(new TypeUsage(type, -1, false));
                                    }
                                }
                            }
                        }
                    } else if ("@arr".equals(kind) && lObject instanceof JsArray) {
                        lastResolvedTypes.addAll(((JsArray) lObject).getTypesInArray());
                    } else {
                        Collection<? extends Type> lastTypeAssignment = lObject.getAssignmentForOffset(offset);
                        lastResolvedObjects.add(lObject);
                        if (!lastTypeAssignment.isEmpty()) {
                            resolveAssignments(model, lObject, offset, lastResolvedObjects, lastResolvedTypes);
                            break;
                        }
                    }
                }
            }
        } else {
            List<JsObject> newResolvedObjects = new ArrayList<JsObject>();
            List<TypeUsage> newResolvedTypes = new ArrayList<TypeUsage>();
            for (JsObject localObject : lastResolvedObjects) {
                JsObject property = ((JsObject) localObject).getProperty(name);
                if (property != null) {
                    if ("@mtd".equals(kind)) {
                        if (property.getJSKind().isFunction()) {
                            Collection<? extends TypeUsage> resovledTypes = ((JsFunction) property).getReturnTypes();
                            newResolvedTypes.addAll(resovledTypes);
                        }
                    } else if ("@arr".equals(kind)) {
                        if (property instanceof JsArray) {
                            newResolvedTypes.addAll(((JsArray) property).getTypesInArray());
                        }
                    } else {
                        Collection<? extends TypeUsage> lastTypeAssignment = property.getAssignmentForOffset(offset);
                        if (lastTypeAssignment.isEmpty()) {
                            newResolvedObjects.add(property);
                        } else {
                            newResolvedTypes.addAll(lastTypeAssignment);
                            if(!property.getProperties().isEmpty()) {
                                newResolvedObjects.add(property);
                            }
                        }
                    }
                }
            }
            for (TypeUsage typeUsage : lastResolvedTypes) {
                if (jsIndex != null) {
                    Collection<String> prototypeChain = new ArrayList<String>();
                    String typeName = typeUsage.getType();
                    if (typeName.contains(SemiTypeResolverVisitor.ST_EXP)) {
                        typeName = typeName.substring(typeName.indexOf(SemiTypeResolverVisitor.ST_EXP) + SemiTypeResolverVisitor.ST_EXP.length());
                    }
                    if (typeName.contains(SemiTypeResolverVisitor.ST_PRO)) {
                        typeName = typeName.replace(SemiTypeResolverVisitor.ST_PRO, ".");
                    }
                    prototypeChain.add(typeName);
                    prototypeChain.addAll(findPrototypeChain(typeName, jsIndex));
                    Collection<? extends IndexResult> indexResults = null;
                    String propertyToCheck = null;
                    for (String fqn : prototypeChain) {
                        propertyToCheck = fqn + "." + name;
                        indexResults = jsIndex.findByFqn(propertyToCheck,
                                                         Index.FIELD_FLAG, Index.FIELD_RETURN_TYPES, Index.FIELD_ARRAY_TYPES, Index.FIELD_ASSIGNMENTS);
                        if (indexResults.isEmpty() && !fqn.endsWith(".prototype")) {
                            propertyToCheck = fqn + ".prototype." + name;
                            indexResults = jsIndex.findByFqn(propertyToCheck,
                                                             Index.FIELD_FLAG, Index.FIELD_RETURN_TYPES, Index.FIELD_ARRAY_TYPES, Index.FIELD_ASSIGNMENTS);
                        }
                        if(!indexResults.isEmpty()) {
                            break;
                        }
                        propertyToCheck = null;
                    }
                    boolean checkProperty = (indexResults == null || indexResults.isEmpty()) && !"@mtd".equals(kind);
                    if (indexResults != null) {
                        for (IndexResult indexResult : indexResults) {
                            JsElement.Kind jsKind = IndexedElement.Flag.getJsKind(Integer.parseInt(indexResult.getValue(Index.FIELD_FLAG)));
                            if ("@mtd".equals(kind) && jsKind.isFunction()) {
                                Collection<TypeUsage> resolvedTypes = IndexedElement.getReturnTypes(indexResult);
                                ModelUtils.addUniqueType(newResolvedTypes, resolvedTypes);
                            } else if ("@arr".equals(kind)) {
                                Collection<TypeUsage> resolvedTypes = IndexedElement.getArrayTypes(indexResult);
                                ModelUtils.addUniqueType(newResolvedTypes, resolvedTypes);
                            } else {
                                checkProperty = true;
                            }
                        }
                    }
                    if (checkProperty) {
                        String propertyFQN = propertyToCheck != null ? propertyToCheck : typeName + "." + name;
                        List<TypeUsage> fromAssignment = new ArrayList<TypeUsage>();
                        resolveAssignments(model, jsIndex, propertyFQN, -1, fromAssignment);
                        if (fromAssignment.isEmpty()) {
                            ModelUtils.addUniqueType(newResolvedTypes, new TypeUsage(propertyFQN));
                        } else {
                            ModelUtils.addUniqueType(newResolvedTypes, fromAssignment);
                        }
                    }
                }
                for (JsObject libGlobal : ModelExtender.getDefault().getExtendingGlobalObjects(model.getGlobalObject().getFileObject())) {
                    for (JsObject object : libGlobal.getProperties().values()) {
                        if (object.getName().equals(typeUsage.getType())) {
                            JsObject property = object.getProperty(name);
                            if (property != null) {
                                JsElement.Kind jsKind = property.getJSKind();
                                if ("@mtd".equals(kind) && jsKind.isFunction()) {
                                    newResolvedTypes.addAll(((JsFunction) property).getReturnTypes());
                                } else {
                                    newResolvedObjects.add(property);
                                }
                            }
                            break;
                        }
                    }
                }
            }
            lastResolvedObjects = newResolvedObjects;
            lastResolvedTypes = newResolvedTypes;
        }
    }
    HashMap<String, TypeUsage> resultTypes  = new HashMap<String, TypeUsage> ();
    for (TypeUsage typeUsage : lastResolvedTypes) {
        if(!resultTypes.containsKey(typeUsage.getType())) {
            resultTypes.put(typeUsage.getType(), typeUsage);
        }
    }
    for (JsObject jsObject : lastResolvedObjects) {
        if (jsObject.isDeclared()) {
            String fqn = jsObject.getFullyQualifiedName();
            if (!resultTypes.containsKey(fqn)) {
                if (includeAllPossible || hasDeclaredProperty(jsObject)) {
                    resultTypes.put(fqn, new TypeUsage(fqn, offset));
                }
            }
        }
    }
    return resultTypes.values();
}