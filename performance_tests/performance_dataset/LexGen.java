static void DumpGetNextToken() {
    int i;
    ostr.println("");
    ostr.println(staticString + "int curLexState = " + defaultLexState + ";");
    ostr.println(staticString + "int defaultLexState = " + defaultLexState + ";");
    ostr.println(staticString + "int jjnewStateCnt;");
    ostr.println(staticString + "int jjround;");
    ostr.println(staticString + "int jjmatchedPos;");
    ostr.println(staticString + "int jjmatchedKind;");
    ostr.println("");
    ostr.println("/** Get the next Token. */");
    ostr.println("public " + staticString + "Token getNextToken()" +
                 " ");
    ostr.println("{");
    if (hasSpecial) {
        ostr.println("  Token specialToken = null;");
    }
    ostr.println("  Token matchedToken;");
    ostr.println("  int curPos = 0;");
    ostr.println("");
    ostr.println("  EOFLoop :\n  for (;;)");
    ostr.println("  {");
    ostr.println("   try");
    ostr.println("   {");
    ostr.println("      curChar = input_stream.BeginToken();");
    ostr.println("   }");
    ostr.println("   catch(java.io.IOException e)");
    ostr.println("   {");
    if (Options.getDebugTokenManager())
        ostr.println("      debugStream.println(\"Returning the <EOF> token.\");");
    ostr.println("      jjmatchedKind = 0;");
    ostr.println("      matchedToken = jjFillToken();");
    if (hasSpecial)
        ostr.println("      matchedToken.specialToken = specialToken;");
    if (nextStateForEof != null || actForEof != null)
        ostr.println("      TokenLexicalActions(matchedToken);");
    if (Options.getCommonTokenAction())
        ostr.println("      CommonTokenAction(matchedToken);");
    ostr.println("      return matchedToken;");
    ostr.println("   }");
    if (hasMoreActions || hasSkipActions || hasTokenActions) {
        ostr.println("   image = jjimage;");
        ostr.println("   image.setLength(0);");
        ostr.println("   jjimageLen = 0;");
    }
    ostr.println("");
    String prefix = "";
    if (hasMore) {
        ostr.println("   for (;;)");
        ostr.println("   {");
        prefix = "  ";
    }
    String endSwitch = "";
    String caseStr = "";
    // this also sets up the start state of the nfa
    if (maxLexStates > 1) {
        ostr.println(prefix + "   switch(curLexState)");
        ostr.println(prefix + "   {");
        endSwitch = prefix + "   }";
        caseStr = prefix + "     case ";
        prefix += "    ";
    }
    prefix += "   ";
    for(i = 0; i < maxLexStates; i++) {
        if (maxLexStates > 1)
            ostr.println(caseStr + i + ":");
        if (singlesToSkip[i].HasTransitions()) {
            // added the backup(0) to make JIT happy
            ostr.println(prefix + "try { input_stream.backup(0);");
            if (singlesToSkip[i].asciiMoves[0] != 0L &&
                    singlesToSkip[i].asciiMoves[1] != 0L) {
                ostr.println(prefix + "   while ((curChar < 64" + " && (0x" +
                             Long.toHexString(singlesToSkip[i].asciiMoves[0]) +
                             "L & (1L << curChar)) != 0L) || \n" +
                             prefix + "          (curChar >> 6) == 1" +
                             " && (0x" +
                             Long.toHexString(singlesToSkip[i].asciiMoves[1]) +
                             "L & (1L << (curChar & 077))) != 0L)");
            } else if (singlesToSkip[i].asciiMoves[1] == 0L) {
                ostr.println(prefix + "   while (curChar <= " +
                             (int)MaxChar(singlesToSkip[i].asciiMoves[0]) + " && (0x" +
                             Long.toHexString(singlesToSkip[i].asciiMoves[0]) +
                             "L & (1L << curChar)) != 0L)");
            } else if (singlesToSkip[i].asciiMoves[0] == 0L) {
                ostr.println(prefix + "   while (curChar > 63 && curChar <= " +
                             ((int)MaxChar(singlesToSkip[i].asciiMoves[1]) + 64) +
                             " && (0x" +
                             Long.toHexString(singlesToSkip[i].asciiMoves[1]) +
                             "L & (1L << (curChar & 077))) != 0L)");
            }
            if (Options.getDebugTokenManager()) {
                ostr.println(prefix + "{");
                ostr.println("      debugStream.println(" +
                             (maxLexStates > 1 ?
                              "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                             "\"Skipping character : \" + " +
                             "TokenMgrError.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \")\");");
            }
            ostr.println(prefix + "      curChar = input_stream.BeginToken();");
            if (Options.getDebugTokenManager())
                ostr.println(prefix + "}");
            ostr.println(prefix + "}");
            ostr.println(prefix + "catch (java.io.IOException e1) { continue EOFLoop; }");
        }
        if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0) {
            if (Options.getDebugTokenManager())
                ostr.println("      debugStream.println(\"   Matched the empty string as \" + tokenImage[" +
                             initMatch[i] + "] + \" token.\");");
            ostr.println(prefix + "jjmatchedKind = " + initMatch[i] + ";");
            ostr.println(prefix + "jjmatchedPos = -1;");
            ostr.println(prefix + "curPos = 0;");
        } else {
            ostr.println(prefix + "jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
            ostr.println(prefix + "jjmatchedPos = 0;");
        }
        if (Options.getDebugTokenManager())
            ostr.println("      debugStream.println(" +
                         (maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                         "\"Current character : \" + " +
                         "TokenMgrError.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
                         "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
        ostr.println(prefix + "curPos = jjMoveStringLiteralDfa0_" + i + "();");
        if (canMatchAnyChar[i] != -1) {
            if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0)
                ostr.println(prefix + "if (jjmatchedPos < 0 || (jjmatchedPos == 0 && jjmatchedKind > " +
                             canMatchAnyChar[i] + "))");
            else
                ostr.println(prefix + "if (jjmatchedPos == 0 && jjmatchedKind > " +
                             canMatchAnyChar[i] + ")");
            ostr.println(prefix + "{");
            if (Options.getDebugTokenManager())
                ostr.println("           debugStream.println(\"   Current character matched as a \" + tokenImage[" +
                             canMatchAnyChar[i] + "] + \" token.\");");
            ostr.println(prefix + "   jjmatchedKind = " + canMatchAnyChar[i] + ";");
            if (initMatch[i] != Integer.MAX_VALUE && initMatch[i] != 0)
                ostr.println(prefix + "   jjmatchedPos = 0;");
            ostr.println(prefix + "}");
        }
        if (maxLexStates > 1)
            ostr.println(prefix + "break;");
    }
    if (maxLexStates > 1)
        ostr.println(endSwitch);
    else if (maxLexStates == 0)
        ostr.println("       jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
    if (maxLexStates > 1)
        prefix = "  ";
    else
        prefix = "";
    if (maxLexStates > 0) {
        ostr.println(prefix + "   if (jjmatchedKind != 0x" + Integer.toHexString(Integer.MAX_VALUE) + ")");
        ostr.println(prefix + "   {");
        ostr.println(prefix + "      if (jjmatchedPos + 1 < curPos)");
        if (Options.getDebugTokenManager()) {
            ostr.println(prefix + "      {");
            ostr.println(prefix + "         debugStream.println(" +
                         "\"   Putting back \" + (curPos - jjmatchedPos - 1) + \" characters into the input stream.\");");
        }
        ostr.println(prefix + "         input_stream.backup(curPos - jjmatchedPos - 1);");
        if (Options.getDebugTokenManager())
            ostr.println(prefix + "      }");
        if (Options.getDebugTokenManager()) {
            if (Options.getJavaUnicodeEscape() ||
                    Options.getUserCharStream())
                ostr.println("    debugStream.println(" +
                             "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH " +
                             "(\" + TokenMgrError.addEscapes(new String(input_stream.GetSuffix(jjmatchedPos + 1))) + " +
                             "\") ******\\n\");");
            else
                ostr.println("    debugStream.println(" +
                             "\"****** FOUND A \" + tokenImage[jjmatchedKind] + \" MATCH " +
                             "(\" + TokenMgrError.addEscapes(new String(input_stream.GetSuffix(jjmatchedPos + 1))) + " +
                             "\") ******\\n\");");
        }
        if (hasSkip || hasMore || hasSpecial) {
            ostr.println(prefix + "      if ((jjtoToken[jjmatchedKind >> 6] & " +
                         "(1L << (jjmatchedKind & 077))) != 0L)");
            ostr.println(prefix + "      {");
        }
        ostr.println(prefix + "         matchedToken = jjFillToken();");
        if (hasSpecial)
            ostr.println(prefix + "         matchedToken.specialToken = specialToken;");
        if (hasTokenActions)
            ostr.println(prefix + "         TokenLexicalActions(matchedToken);");
        if (maxLexStates > 1) {
            ostr.println("       if (jjnewLexState[jjmatchedKind] != -1)");
            ostr.println(prefix + "       curLexState = jjnewLexState[jjmatchedKind];");
        }
        if (Options.getCommonTokenAction())
            ostr.println(prefix + "         CommonTokenAction(matchedToken);");
        ostr.println(prefix + "         return matchedToken;");
        if (hasSkip || hasMore || hasSpecial) {
            ostr.println(prefix + "      }");
            if (hasSkip || hasSpecial) {
                if (hasMore) {
                    ostr.println(prefix + "      else if ((jjtoSkip[jjmatchedKind >> 6] & " +
                                 "(1L << (jjmatchedKind & 077))) != 0L)");
                } else
                    ostr.println(prefix + "      else");
                ostr.println(prefix + "      {");
                if (hasSpecial) {
                    ostr.println(prefix + "         if ((jjtoSpecial[jjmatchedKind >> 6] & " +
                                 "(1L << (jjmatchedKind & 077))) != 0L)");
                    ostr.println(prefix + "         {");
                    ostr.println(prefix + "            matchedToken = jjFillToken();");
                    ostr.println(prefix + "            if (specialToken == null)");
                    ostr.println(prefix + "               specialToken = matchedToken;");
                    ostr.println(prefix + "            else");
                    ostr.println(prefix + "            {");
                    ostr.println(prefix + "               matchedToken.specialToken = specialToken;");
                    ostr.println(prefix + "               specialToken = (specialToken.next = matchedToken);");
                    ostr.println(prefix + "            }");
                    if (hasSkipActions)
                        ostr.println(prefix + "            SkipLexicalActions(matchedToken);");
                    ostr.println(prefix + "         }");
                    if (hasSkipActions) {
                        ostr.println(prefix + "         else");
                        ostr.println(prefix + "            SkipLexicalActions(null);");
                    }
                } else if (hasSkipActions)
                    ostr.println(prefix + "         SkipLexicalActions(null);");
                if (maxLexStates > 1) {
                    ostr.println("         if (jjnewLexState[jjmatchedKind] != -1)");
                    ostr.println(prefix + "         curLexState = jjnewLexState[jjmatchedKind];");
                }
                ostr.println(prefix + "         continue EOFLoop;");
                ostr.println(prefix + "      }");
            }
            if (hasMore) {
                if (hasMoreActions)
                    ostr.println(prefix + "      MoreLexicalActions();");
                else if (hasSkipActions || hasTokenActions)
                    ostr.println(prefix + "      jjimageLen += jjmatchedPos + 1;");
                if (maxLexStates > 1) {
                    ostr.println("      if (jjnewLexState[jjmatchedKind] != -1)");
                    ostr.println(prefix + "      curLexState = jjnewLexState[jjmatchedKind];");
                }
                ostr.println(prefix + "      curPos = 0;");
                ostr.println(prefix + "      jjmatchedKind = 0x" + Integer.toHexString(Integer.MAX_VALUE) + ";");
                ostr.println(prefix + "      try {");
                ostr.println(prefix + "         curChar = input_stream.readChar();");
                if (Options.getDebugTokenManager())
                    ostr.println("   debugStream.println(" +
                                 (maxLexStates > 1 ? "\"<\" + lexStateNames[curLexState] + \">\" + " : "") +
                                 "\"Current character : \" + " +
                                 "TokenMgrError.addEscapes(String.valueOf(curChar)) + \" (\" + (int)curChar + \") " +
                                 "at line \" + input_stream.getEndLine() + \" column \" + input_stream.getEndColumn());");
                ostr.println(prefix + "         continue;");
                ostr.println(prefix + "      }");
                ostr.println(prefix + "      catch (java.io.IOException e1) { }");
            }
        }
        ostr.println(prefix + "   }");
        ostr.println(prefix + "   int error_line = input_stream.getEndLine();");
        ostr.println(prefix + "   int error_column = input_stream.getEndColumn();");
        ostr.println(prefix + "   String error_after = null;");
        ostr.println(prefix + "   boolean EOFSeen = false;");
        ostr.println(prefix + "   try { input_stream.readChar(); input_stream.backup(1); }");
        ostr.println(prefix + "   catch (java.io.IOException e1) {");
        ostr.println(prefix + "      EOFSeen = true;");
        ostr.println(prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.GetImage();");
        ostr.println(prefix + "      if (curChar == '\\n' || curChar == '\\r') {");
        ostr.println(prefix + "         error_line++;");
        ostr.println(prefix + "         error_column = 0;");
        ostr.println(prefix + "      }");
        ostr.println(prefix + "      else");
        ostr.println(prefix + "         error_column++;");
        ostr.println(prefix + "   }");
        ostr.println(prefix + "   if (!EOFSeen) {");
        ostr.println(prefix + "      input_stream.backup(1);");
        ostr.println(prefix + "      error_after = curPos <= 1 ? \"\" : input_stream.GetImage();");
        ostr.println(prefix + "   }");
        ostr.println(prefix + "   throw new TokenMgrError(" +
                     "EOFSeen, curLexState, error_line, error_column, error_after, curChar, TokenMgrError.LEXICAL_ERROR);");
    }
    if (hasMore)
        ostr.println(prefix + " }");
    ostr.println("  }");
    ostr.println("}");
    ostr.println("");
}