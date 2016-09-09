package org.python.core;

public enum CompareOp {
    LT(0), LE(1), EQ(2), NE(3), GT(4), GE(5);
    private int n;

    private static final CompareOp[] swappedOps = new CompareOp[] {
            GT, GE, EQ, NE, LT, LE
    };
    private static final String[] stringOps = new String[] {
        "<", "<=", "==", "!=", ">", ">="
    };
    private static final String[] methOps = new String[] {
        "__lt__", "__le__", "__eq__", "__ne__", "__gt__", "__ge__"
    };

    CompareOp(int n) {
        this.n = n;
    }

    public CompareOp reflectedOp() {
        return swappedOps[n];
    }

    public PyObject bool(int result) {
        if (result < -1) {
//            if (this == EQ) {
//                return Py.False;
//            } else if (this == NE) {
//                return Py.True;
//            }
            return Py.NotImplemented;
        }
        switch(this) {
            case LT:
                return Py.newBoolean(result < 0);
            case LE:
                return Py.newBoolean(result <= 0);
            case EQ:
                return Py.newBoolean(result == 0);
            case NE:
                return Py.newBoolean(result != 0);
            case GT:
                return Py.newBoolean(result > 0);
            case GE:
                return Py.newBoolean(result >= 0);
            default:
                return Py.NotImplemented;
        }
    }

    public PyObject neq() {
        switch (this) {
            case EQ:
                return Py.False;
            case NE:
                return Py.True;
            default:
                return Py.NotImplemented;
        }
    }

    public String toString() {
        return stringOps[n];
    }

    public String meth() {
        return methOps[n];
    }
}
