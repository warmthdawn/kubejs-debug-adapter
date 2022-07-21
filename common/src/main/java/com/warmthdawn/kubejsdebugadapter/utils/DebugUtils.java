package com.warmthdawn.kubejsdebugadapter.utils;

import dev.latvian.mods.rhino.Kit;
import dev.latvian.mods.rhino.Token;
import dev.latvian.mods.rhino.ast.FunctionNode;
import dev.latvian.mods.rhino.ast.RegExpLiteral;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.math.BigInteger;

public class DebugUtils {
    static final int

        // delete operator used on a name
        Icode_DELNAME = 0;

    static final int// Stack: ... value1 -> ... value1 value1
        Icode_DUP = -1;

    static final int// Stack: ... value2 value1 -> ... value2 value1 value2 value1
        Icode_DUP2 = -2;

    static final int// Stack: ... value2 value1 -> ... value1 value2
        Icode_SWAP = -3;

    static final int// Stack: ... value1 -> ...
        Icode_POP = -4;

    static final int// Store stack top into return register and then pop it
        Icode_POP_RESULT = -5;

    static final int// To jump conditionally and pop additional stack value
        Icode_IFEQ_POP = -6;

    static final int// various types of ++/--
        Icode_VAR_INC_DEC = -7;
    static final int Icode_NAME_INC_DEC = -8;
    static final int Icode_PROP_INC_DEC = -9;
    static final int Icode_ELEM_INC_DEC = -10;
    static final int Icode_REF_INC_DEC = -11;

    static final int// load/save scope from/to local
        Icode_SCOPE_LOAD = -12;
    static final int Icode_SCOPE_SAVE = -13;

    static final int Icode_TYPEOFNAME = -14;

    static final int// helper for function calls
        Icode_NAME_AND_THIS = -15;
    static final int Icode_PROP_AND_THIS = -16;
    static final int Icode_ELEM_AND_THIS = -17;
    static final int Icode_VALUE_AND_THIS = -18;

    static final int// Create closure object for nested functions
        Icode_CLOSURE_EXPR = -19;
    static final int Icode_CLOSURE_STMT = -20;

    static final int// Special calls
        Icode_CALLSPECIAL = -21;

    static final int// To return undefined value
        Icode_RETUNDEF = -22;

    static final int// Exception handling implementation
        Icode_GOSUB = -23;
    static final int Icode_STARTSUB = -24;
    static final int Icode_RETSUB = -25;

    static final int// To indicating a line number change in icodes.
        Icode_LINE = -26;

    static final int// To store shorts and ints inline
        Icode_SHORTNUMBER = -27;
    static final int Icode_INTNUMBER = -28;

    static final int// To create and populate array to hold values for [] and {} literals
        Icode_LITERAL_NEW = -29;
    static final int Icode_LITERAL_SET = -30;

    static final int// Array literal with skipped index like [1,,2]
        Icode_SPARE_ARRAYLIT = -31;

    static final int// Load index register to prepare for the following index operation
        Icode_REG_IND_C0 = -32;
    static final int Icode_REG_IND_C1 = -33;
    static final int Icode_REG_IND_C2 = -34;
    static final int Icode_REG_IND_C3 = -35;
    static final int Icode_REG_IND_C4 = -36;
    static final int Icode_REG_IND_C5 = -37;
    static final int Icode_REG_IND1 = -38;
    static final int Icode_REG_IND2 = -39;
    static final int Icode_REG_IND4 = -40;

    static final int// Load string register to prepare for the following string operation
        Icode_REG_STR_C0 = -41;
    static final int Icode_REG_STR_C1 = -42;
    static final int Icode_REG_STR_C2 = -43;
    static final int Icode_REG_STR_C3 = -44;
    static final int Icode_REG_STR1 = -45;
    static final int Icode_REG_STR2 = -46;
    static final int Icode_REG_STR4 = -47;

    static final int// Version of getvar/setvar that read var index directly from bytecode
        Icode_GETVAR1 = -48;
    static final int Icode_SETVAR1 = -49;

    static final int// Load undefined
        Icode_UNDEF = -50;
    static final int Icode_ZERO = -51;
    static final int Icode_ONE = -52;

    static final int// entrance and exit from .()
        Icode_ENTERDQ = -53;
    static final int Icode_LEAVEDQ = -54;

    static final int Icode_TAIL_CALL = -55;

    static final int// Clear local to allow GC its context
        Icode_LOCAL_CLEAR = -56;

    static final int// Literal get/set
        Icode_LITERAL_GETTER = -57;
    static final int Icode_LITERAL_SETTER = -58;

    static final int// const
        Icode_SETCONST = -59;
    static final int Icode_SETCONSTVAR = -60;
    static final int Icode_SETCONSTVAR1 = -61;

    static final int// Generator opcodes (along with Token.YIELD)
        Icode_GENERATOR = -62;
    static final int Icode_GENERATOR_END = -63;

    // Icode_DEBUGGER = -64,

    static final int Icode_GENERATOR_RETURN = -65;
    static final int Icode_YIELD_STAR = -66;

    static final int// Call to GetTemplateLiteralCallSite
        Icode_TEMPLATE_LITERAL_CALLSITE = -67;
    public static final int Icode_STATEMENT_BREAK = -68;
    public static final int Icode_EXPRESSION_BREAK = -69;

    static String bytecodeName(int bytecode) {
        switch (bytecode) {
            case Icode_DUP:
                return "DUP";
            case Icode_DUP2:
                return "DUP2";
            case Icode_SWAP:
                return "SWAP";
            case Icode_POP:
                return "POP";
            case Icode_POP_RESULT:
                return "POP_RESULT";
            case Icode_IFEQ_POP:
                return "IFEQ_POP";
            case Icode_VAR_INC_DEC:
                return "VAR_INC_DEC";
            case Icode_NAME_INC_DEC:
                return "NAME_INC_DEC";
            case Icode_PROP_INC_DEC:
                return "PROP_INC_DEC";
            case Icode_ELEM_INC_DEC:
                return "ELEM_INC_DEC";
            case Icode_REF_INC_DEC:
                return "REF_INC_DEC";
            case Icode_SCOPE_LOAD:
                return "SCOPE_LOAD";
            case Icode_SCOPE_SAVE:
                return "SCOPE_SAVE";
            case Icode_TYPEOFNAME:
                return "TYPEOFNAME";
            case Icode_NAME_AND_THIS:
                return "NAME_AND_THIS";
            case Icode_PROP_AND_THIS:
                return "PROP_AND_THIS";
            case Icode_ELEM_AND_THIS:
                return "ELEM_AND_THIS";
            case Icode_VALUE_AND_THIS:
                return "VALUE_AND_THIS";
            case Icode_CLOSURE_EXPR:
                return "CLOSURE_EXPR";
            case Icode_CLOSURE_STMT:
                return "CLOSURE_STMT";
            case Icode_CALLSPECIAL:
                return "CALLSPECIAL";
            case Icode_RETUNDEF:
                return "RETUNDEF";
            case Icode_GOSUB:
                return "GOSUB";
            case Icode_STARTSUB:
                return "STARTSUB";
            case Icode_RETSUB:
                return "RETSUB";
            case Icode_LINE:
                return "LINE";
            case Icode_SHORTNUMBER:
                return "SHORTNUMBER";
            case Icode_INTNUMBER:
                return "INTNUMBER";
            case Icode_LITERAL_NEW:
                return "LITERAL_NEW";
            case Icode_LITERAL_SET:
                return "LITERAL_SET";
            case Icode_SPARE_ARRAYLIT:
                return "SPARE_ARRAYLIT";
            case Icode_REG_IND_C0:
                return "REG_IND_C0";
            case Icode_REG_IND_C1:
                return "REG_IND_C1";
            case Icode_REG_IND_C2:
                return "REG_IND_C2";
            case Icode_REG_IND_C3:
                return "REG_IND_C3";
            case Icode_REG_IND_C4:
                return "REG_IND_C4";
            case Icode_REG_IND_C5:
                return "REG_IND_C5";
            case Icode_REG_IND1:
                return "LOAD_IND1";
            case Icode_REG_IND2:
                return "LOAD_IND2";
            case Icode_REG_IND4:
                return "LOAD_IND4";
            case Icode_REG_STR_C0:
                return "REG_STR_C0";
            case Icode_REG_STR_C1:
                return "REG_STR_C1";
            case Icode_REG_STR_C2:
                return "REG_STR_C2";
            case Icode_REG_STR_C3:
                return "REG_STR_C3";
            case Icode_REG_STR1:
                return "LOAD_STR1";
            case Icode_REG_STR2:
                return "LOAD_STR2";
            case Icode_REG_STR4:
                return "LOAD_STR4";
            case Icode_GETVAR1:
                return "GETVAR1";
            case Icode_SETVAR1:
                return "SETVAR1";
            case Icode_UNDEF:
                return "UNDEF";
            case Icode_ZERO:
                return "ZERO";
            case Icode_ONE:
                return "ONE";
            case Icode_ENTERDQ:
                return "ENTERDQ";
            case Icode_LEAVEDQ:
                return "LEAVEDQ";
            case Icode_TAIL_CALL:
                return "TAIL_CALL";
            case Icode_LOCAL_CLEAR:
                return "LOCAL_CLEAR";
            case Icode_LITERAL_GETTER:
                return "LITERAL_GETTER";
            case Icode_LITERAL_SETTER:
                return "LITERAL_SETTER";
            case Icode_SETCONST:
                return "SETCONST";
            case Icode_SETCONSTVAR:
                return "SETCONSTVAR";
            case Icode_SETCONSTVAR1:
                return "SETCONSTVAR1";
            case Icode_GENERATOR:
                return "GENERATOR";
            case Icode_GENERATOR_END:
                return "GENERATOR_END";
            case Icode_GENERATOR_RETURN:
                return "GENERATOR_RETURN";
            case Icode_YIELD_STAR:
                return "YIELD_STAR";
            case Icode_TEMPLATE_LITERAL_CALLSITE:
                return "TEMPLATE_LITERAL_CALLSITE";
            case Icode_STATEMENT_BREAK:
                return "STATEMENT_BREAK";
            case Icode_EXPRESSION_BREAK:
                return "EXPRESSION_BREAK";
        }
        return null;
    }


    public static void dumpICode(Object idata) throws Exception {
        Class<?> clazz = Class.forName("dev.latvian.mods.rhino.InterpreterData");
        // itsICode
        Field itsICode = clazz.getDeclaredField("itsICode");
        itsICode.setAccessible(true);
        byte[] iCode = (byte[]) itsICode.get(idata);

        // itsStringTable
        Field itsStringTable = clazz.getDeclaredField("itsStringTable");
        itsStringTable.setAccessible(true);
        String[] strings = (String[]) itsStringTable.get(idata);

        // itsRegExpLiterals
        Field itsRegExpLiterals = clazz.getDeclaredField("itsRegExpLiterals");
        itsRegExpLiterals.setAccessible(true);
        RegExpLiteral[] regExpLiterals = (RegExpLiteral[]) itsRegExpLiterals.get(idata);

        // literalIds
        Field literalIds = clazz.getDeclaredField("literalIds");
        literalIds.setAccessible(true);
        int[] literalIdsArray = (int[]) literalIds.get(idata);

        // itsNestedFunctions
        Field itsNestedFunctions = clazz.getDeclaredField("itsNestedFunctions");
        itsNestedFunctions.setAccessible(true);
        Object[] functions = (Object[]) itsNestedFunctions.get(idata);

        // itsDoubleTable
        Field itsDoubleTable = clazz.getDeclaredField("itsDoubleTable");
        itsDoubleTable.setAccessible(true);
        double[] doubles = (double[]) itsDoubleTable.get(idata);

        int iCodeLength = iCode.length;
        PrintStream out = System.out;

        int indexReg = 0;
        for (int pc = 0; pc < iCodeLength; ) {
            out.flush();
            out.print(" [" + pc + "] ");
            int token = iCode[pc];
            int icodeLength = bytecodeSpan(token);
            String tname = bytecodeName(token);
            int old_pc = pc;
            ++pc;
            switch (token) {
                default:
                    if (icodeLength != 1) Kit.codeBug();
                    out.println(tname);
                    break;

                case Icode_GOSUB:
                case Token.GOTO:
                case Token.IFEQ:
                case Token.IFNE:
                case Icode_IFEQ_POP:
                case Icode_LEAVEDQ: {
                    int newPC = pc + getShort(iCode, pc) - 1;
                    out.println(tname + " " + newPC);
                    pc += 2;
                    break;
                }
                case Icode_VAR_INC_DEC:
                case Icode_NAME_INC_DEC:
                case Icode_PROP_INC_DEC:
                case Icode_ELEM_INC_DEC:
                case Icode_REF_INC_DEC: {
                    int incrDecrType = iCode[pc];
                    out.println(tname + " " + incrDecrType);
                    ++pc;
                    break;
                }

                case Icode_CALLSPECIAL: {
                    int callType = iCode[pc] & 0xFF;
                    boolean isNew = (iCode[pc + 1] != 0);
                    int line = getIndex(iCode, pc + 2);
                    out.println(
                        tname + " " + callType + " " + isNew + " " + indexReg + " " + line);
                    pc += 4;
                    break;
                }

                case Token.CATCH_SCOPE: {
                    boolean afterFisrtFlag = (iCode[pc] != 0);
                    out.println(tname + " " + afterFisrtFlag);
                    ++pc;
                }
                break;
                case Token.REGEXP:
                    out.println(tname + " " + regExpLiterals[indexReg]);
                    break;
                case Token.OBJECTLIT:
                case Icode_SPARE_ARRAYLIT:
                    out.println(tname + " " + literalIdsArray[indexReg]);
                    break;
                case Icode_CLOSURE_EXPR:
                case Icode_CLOSURE_STMT:
                    out.println(tname + " " + functions[indexReg]);
                    break;
                case Token.CALL:
                case Icode_TAIL_CALL:
                case Token.REF_CALL:
                case Token.NEW:
                    out.println(tname + ' ' + indexReg);
                    break;
                case Token.THROW:
                case Token.YIELD:
                case Icode_YIELD_STAR:
                case Icode_GENERATOR:
                case Icode_GENERATOR_END:
                case Icode_GENERATOR_RETURN: {
                    int line = getIndex(iCode, pc);
                    out.println(tname + " : " + line);
                    pc += 2;
                    break;
                }
                case Icode_SHORTNUMBER: {
                    int value = getShort(iCode, pc);
                    out.println(tname + " " + value);
                    pc += 2;
                    break;
                }
                case Icode_INTNUMBER: {
                    int value = getInt(iCode, pc);
                    out.println(tname + " " + value);
                    pc += 4;
                    break;
                }
                case Token.NUMBER: {
                    double value = doubles[indexReg];
                    out.println(tname + " " + value);
                    break;
                }
                case Icode_LINE: {
                    int line = getIndex(iCode, pc);
                    out.println(tname + " : " + line);
                    pc += 2;
                    break;
                }
                case Icode_REG_STR_C0: {
                    String str = strings[0];
                    out.println(tname + " \"" + str + '"');
                    break;
                }
                case Icode_REG_STR_C1: {
                    String str = strings[1];
                    out.println(tname + " \"" + str + '"');
                    break;
                }
                case Icode_REG_STR_C2: {
                    String str = strings[2];
                    out.println(tname + " \"" + str + '"');
                    break;
                }
                case Icode_REG_STR_C3: {
                    String str = strings[3];
                    out.println(tname + " \"" + str + '"');
                    break;
                }
                case Icode_REG_STR1: {
                    String str = strings[0xFF & iCode[pc]];
                    out.println(tname + " \"" + str + '"');
                    ++pc;
                    break;
                }
                case Icode_REG_STR2: {
                    String str = strings[getIndex(iCode, pc)];
                    out.println(tname + " \"" + str + '"');
                    pc += 2;
                    break;
                }
                case Icode_REG_STR4: {
                    String str = strings[getInt(iCode, pc)];
                    out.println(tname + " \"" + str + '"');
                    pc += 4;
                    break;
                }
                case Icode_REG_IND_C0:
                    indexReg = 0;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C1:
                    indexReg = 1;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C2:
                    indexReg = 2;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C3:
                    indexReg = 3;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C4:
                    indexReg = 4;
                    out.println(tname);
                    break;
                case Icode_REG_IND_C5:
                    indexReg = 5;
                    out.println(tname);
                    break;
                case Icode_REG_IND1: {
                    indexReg = 0xFF & iCode[pc];
                    out.println(tname + " " + indexReg);
                    ++pc;
                    break;
                }
                case Icode_REG_IND2: {
                    indexReg = getIndex(iCode, pc);
                    out.println(tname + " " + indexReg);
                    pc += 2;
                    break;
                }
                case Icode_REG_IND4: {
                    indexReg = getInt(iCode, pc);
                    out.println(tname + " " + indexReg);
                    pc += 4;
                    break;
                }
                case Icode_GETVAR1:
                case Icode_SETVAR1:
                case Icode_SETCONSTVAR1:
                    indexReg = iCode[pc];
                    out.println(tname + " " + indexReg);
                    ++pc;
                    break;
                case Icode_STATEMENT_BREAK:
                    out.println(tname);
                    pc += 2;
                    break;
                case Icode_EXPRESSION_BREAK:
                    out.println(tname);
                    pc += 2;
                    break;

                case Token.NAME:
                    out.println("NAME");
                    break;

            }
            if (old_pc + icodeLength != pc) Kit.codeBug();
        }

        out.flush();
    }


    private static int getShort(byte[] iCode, int pc) {
        return (iCode[pc] << 8) | (iCode[pc + 1] & 0xFF);
    }

    private static int getIndex(byte[] iCode, int pc) {
        return ((iCode[pc] & 0xFF) << 8) | (iCode[pc + 1] & 0xFF);
    }

    private static int getInt(byte[] iCode, int pc) {
        return (iCode[pc] << 24) | ((iCode[pc + 1] & 0xFF) << 16) | ((iCode[pc + 2] & 0xFF) << 8) | (iCode[pc + 3] & 0xFF);
    }


    private static int bytecodeSpan(int bytecode) {
        switch (bytecode) {
            case Token.THROW:
            case Token.YIELD:
            case Icode_YIELD_STAR:
            case Icode_GENERATOR:
            case Icode_GENERATOR_END:
            case Icode_GENERATOR_RETURN:
                // source line
                return 1 + 2;

            case Icode_GOSUB:
            case Token.GOTO:
            case Token.IFEQ:
            case Token.IFNE:
            case Icode_IFEQ_POP:
            case Icode_LEAVEDQ:
                // target pc offset
                return 1 + 2;

            case Icode_CALLSPECIAL:
                // call type
                // is new
                // line number
                return 1 + 1 + 1 + 2;

            case Token.CATCH_SCOPE:
                // scope flag
                return 1 + 1;

            case Icode_VAR_INC_DEC:
            case Icode_NAME_INC_DEC:
            case Icode_PROP_INC_DEC:
            case Icode_ELEM_INC_DEC:
            case Icode_REF_INC_DEC:
                // type of ++/--
                return 1 + 1;

            case Icode_SHORTNUMBER:
                // short number
                return 1 + 2;

            case Icode_INTNUMBER:
                // int number
                return 1 + 4;

            case Icode_REG_IND1:
                // ubyte index
                return 1 + 1;

            case Icode_REG_IND2:
                // ushort index
                return 1 + 2;

            case Icode_REG_IND4:
                // int index
                return 1 + 4;

            case Icode_REG_STR1:
                // ubyte string index
                return 1 + 1;

            case Icode_REG_STR2:
                // ushort string index
                return 1 + 2;

            case Icode_REG_STR4:
                // int string index
                return 1 + 4;

            case Icode_GETVAR1:
            case Icode_SETVAR1:
            case Icode_SETCONSTVAR1:
                // byte var index
                return 1 + 1;

            case Icode_LINE:
                // line number
                return 1 + 2;
            case Icode_STATEMENT_BREAK:
                // line number
                return 1 + 2;
            case Icode_EXPRESSION_BREAK:
                // line number
                return 1 + 2;
        }
        return 1;
    }

}
