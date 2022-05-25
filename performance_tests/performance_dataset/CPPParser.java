public final void function_direct_declarator() throws RecognitionException, TokenStreamException {
    String q="";
    {
        switch ( LA(1)) {
        case LPAREN: {
            match(LPAREN);
            q=qualified_id();
            if ( inputState.guessing==0 ) {
                declaratorID(q,CPPvariables.QI_FUN);
            }
            match(RPAREN);
            break;
        }
        case ID:
        case OPERATOR:
        case LITERAL_this:
        case LITERAL_true:
        case LITERAL_false:
        case SCOPE: {
            q=qualified_id();
            if ( inputState.guessing==0 ) {
                declaratorID(q,CPPvariables.QI_FUN);
            }
            break;
        }
        default: {
            throw new NoViableAltException(LT(1), getFilename());
        }
        }
    }
    if ( inputState.guessing==0 ) {
        m.functionDirectDeclarator(q);
    }
    match(LPAREN);
    {
        switch ( LA(1)) {
        case LITERAL_typedef:
        case LITERAL_enum:
        case ID:
        case LITERAL_inline:
        case LITERAL_extern:
        case LITERAL__inline:
        case LITERAL___inline:
        case LITERAL_virtual:
        case LITERAL_explicit:
        case LITERAL_friend:
        case LITERAL__stdcall:
        case LITERAL___stdcall:
        case LITERAL__declspec:
        case LITERAL___declspec:
        case LPAREN:
        case LITERAL_typename:
        case LITERAL_auto:
        case LITERAL_register:
        case LITERAL_static:
        case LITERAL_mutable:
        case LITERAL_const:
        case LITERAL_const_cast:
        case LITERAL_volatile:
        case LITERAL_char:
        case LITERAL_wchar_t:
        case LITERAL_bool:
        case LITERAL_short:
        case LITERAL_int:
        case 44:
        case 45:
        case 46:
        case LITERAL_long:
        case LITERAL_signed:
        case LITERAL_unsigned:
        case LITERAL_float:
        case LITERAL_double:
        case LITERAL_void:
        case LITERAL_class:
        case LITERAL_struct:
        case LITERAL_union:
        case OPERATOR:
        case LITERAL_this:
        case LITERAL_true:
        case LITERAL_false:
        case STAR:
        case AMPERSAND:
        case TILDE:
        case ELLIPSIS:
        case SCOPE:
        case LITERAL__cdecl:
        case LITERAL___cdecl:
        case LITERAL__near:
        case LITERAL___near:
        case LITERAL__far:
        case LITERAL___far:
        case LITERAL___interrupt:
        case LITERAL_pascal:
        case LITERAL__pascal:
        case LITERAL___pascal: {
            parameter_list();
            break;
        }
        case RPAREN: {
            break;
        }
        default: {
            throw new NoViableAltException(LT(1), getFilename());
        }
        }
    }
    match(RPAREN);
    {
        _loop232:
        do {
            if (((LA(1) >= LITERAL_const && LA(1) <= LITERAL_volatile)) && (_tokenSet_47.member(LA(2)))) {
                type_qualifier();
            } else {
                break _loop232;
            }
        } while (true);
    }
    {
        switch ( LA(1)) {
        case ASSIGNEQUAL: {
            match(ASSIGNEQUAL);
            match(OCTALINT);
            break;
        }
        case LITERAL_typedef:
        case LITERAL_enum:
        case ID:
        case LCURLY:
        case SEMICOLON:
        case LITERAL_inline:
        case LITERAL_extern:
        case LITERAL__inline:
        case LITERAL___inline:
        case LITERAL_virtual:
        case LITERAL_explicit:
        case LITERAL_friend:
        case LITERAL__stdcall:
        case LITERAL___stdcall:
        case LITERAL__declspec:
        case LITERAL___declspec:
        case LITERAL_typename:
        case LITERAL_auto:
        case LITERAL_register:
        case LITERAL_static:
        case LITERAL_mutable:
        case LITERAL_const:
        case LITERAL_const_cast:
        case LITERAL_volatile:
        case LITERAL_char:
        case LITERAL_wchar_t:
        case LITERAL_bool:
        case LITERAL_short:
        case LITERAL_int:
        case 44:
        case 45:
        case 46:
        case LITERAL_long:
        case LITERAL_signed:
        case LITERAL_unsigned:
        case LITERAL_float:
        case LITERAL_double:
        case LITERAL_void:
        case LITERAL_class:
        case LITERAL_struct:
        case LITERAL_union:
        case LITERAL_throw:
        case LITERAL_using:
        case SCOPE: {
            break;
        }
        default: {
            throw new NoViableAltException(LT(1), getFilename());
        }
        }
    }
    {
        switch ( LA(1)) {
        case LITERAL_throw: {
            exception_specification();
            break;
        }
        case LITERAL_typedef:
        case LITERAL_enum:
        case ID:
        case LCURLY:
        case SEMICOLON:
        case LITERAL_inline:
        case LITERAL_extern:
        case LITERAL__inline:
        case LITERAL___inline:
        case LITERAL_virtual:
        case LITERAL_explicit:
        case LITERAL_friend:
        case LITERAL__stdcall:
        case LITERAL___stdcall:
        case LITERAL__declspec:
        case LITERAL___declspec:
        case LITERAL_typename:
        case LITERAL_auto:
        case LITERAL_register:
        case LITERAL_static:
        case LITERAL_mutable:
        case LITERAL_const:
        case LITERAL_const_cast:
        case LITERAL_volatile:
        case LITERAL_char:
        case LITERAL_wchar_t:
        case LITERAL_bool:
        case LITERAL_short:
        case LITERAL_int:
        case 44:
        case 45:
        case 46:
        case LITERAL_long:
        case LITERAL_signed:
        case LITERAL_unsigned:
        case LITERAL_float:
        case LITERAL_double:
        case LITERAL_void:
        case LITERAL_class:
        case LITERAL_struct:
        case LITERAL_union:
        case LITERAL_using:
        case SCOPE: {
            break;
        }
        default: {
            throw new NoViableAltException(LT(1), getFilename());
        }
        }
    }
}