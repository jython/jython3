// Copyright (c) Corporation for National Research Initiatives

package org.python.compiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.python.antlr.ParseException;
import org.python.antlr.Visitor;
import org.python.antlr.ast.Assign;
import org.python.antlr.ast.Name;
import org.python.antlr.ast.Suite;
import org.python.antlr.ast.Tuple;
import org.python.antlr.ast.arguments;
import org.python.antlr.ast.expr_contextType;
import org.python.antlr.base.expr;
import org.python.antlr.base.stmt;

public class ArgListCompiler extends Visitor
{
    public boolean arglist, keywordlist;
    public List<expr> defaults;
    public List<String> names;
    public Map<String, expr> kw_defaults;
    public int kwonlyargcount;
    public List<String> fpnames;
    public List<stmt> init_code;

    public ArgListCompiler() {
        arglist = keywordlist = false;
        defaults = null;
        names = new ArrayList<String>();
        kw_defaults = new HashMap<>();
        fpnames = new ArrayList<String>();
        init_code = new ArrayList<stmt>();
    }

    public void reset() {
        arglist = keywordlist = false;
        defaults = null;
        names.clear();
        init_code.clear();
    }

    public void appendInitCode(Suite node) {
        node.getInternalBody().addAll(0, init_code);
    }

    public List<expr> getDefaults() {
        return defaults;
    }

    public void visitArgs(arguments args) throws Exception {
        for (int i = 0; i < args.getInternalArgs().size(); i++) {
            String name = (String) visit(args.getInternalArgs().get(i));
            names.add(name);
            if (args.getInternalArgs().get(i) instanceof Tuple) {
                List<expr> targets = new ArrayList<expr>();
                targets.add(args.getInternalArgs().get(i));
                Assign ass = new Assign(args.getInternalArgs().get(i),
                    targets,
                    new Name(args.getInternalArgs().get(i), name, expr_contextType.Load));
                init_code.add(ass);
            }
        }
        if (args.getInternalVararg() != null) {
            arglist = true;
            names.add(args.getInternalVararg());
        }
        kwonlyargcount = args.getInternalKwonlyargs().size();
        names.addAll(args.getInternalKwonlyargs());
        if (args.getInternalKwarg() != null) {
            keywordlist = true;
            names.add(args.getInternalKwarg());
        }

        List<String> kwonlyargs = args.getInternalKwonlyargs();
        List<expr> kwdefaults = args.getInternalKw_defaults();
        for (int i = 0; i < kwonlyargcount; i++) {
            expr kwDefault = kwdefaults.get(i);
            if (kwDefault != null) {
                kw_defaults.put(kwonlyargs.get(i), kwDefault);
            }
        }

        defaults = new ArrayList<expr>();
        List<expr> internalDefaults = args.getInternalDefaults();
        for (int i = 0; i < internalDefaults.size(); i++) {
            expr val = internalDefaults.get(i);
            if (val == null && ! defaults.isEmpty()) {
                 throw new ParseException(
                    "non-default argument follows default argument",
                    args.getInternalArgs().get(args.getInternalArgs().size() - defaults.size() + i));
            }
            if (val != null) {
                defaults.add(val);
            }
        }
    }

    @Override
    public Object visitName(Name node) throws Exception {
        //FIXME: do we need Store and Param, or just Param?
        if (node.getInternalCtx() != expr_contextType.Store && node.getInternalCtx() != expr_contextType.Param) {
            return null;
        } 

        if (fpnames.contains(node.getInternalId())) {
            throw new ParseException("duplicate argument name found: " +
                                     node.getInternalId(), node);
        }
        fpnames.add(node.getInternalId());
        return node.getInternalId();
    }

    @Override
    public Object visitTuple(Tuple node) throws Exception {
        StringBuffer name = new StringBuffer("(");
        int n = node.getInternalElts().size();
        for (int i = 0; i < n-1; i++) {
            name.append(visit(node.getInternalElts().get(i)));
            name.append(", ");
        }
        name.append(visit(node.getInternalElts().get(n - 1)));
        name.append(")");
        return name.toString();
    }
}
