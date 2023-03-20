package mini_c;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;

public class ToRTL implements Visitor {
    RTLfun fun;
    RTLfile file;
    RTLgraph graph;
    Label entry;
    Register currentRegister;
    HashMap<String,Register> vars = new HashMap<>();

    @Override
    public void visit(Unop n) {

    }

    @Override
    public void visit(Binop n) {

    }

    @Override
    public void visit(String n) {

    }

    @Override
    public void visit(Tint n) {

    }

    @Override
    public void visit(Tstructp n) {

    }

    @Override
    public void visit(Tvoidstar n) {

    }

    @Override
    public void visit(Ttypenull n) {

    }

    @Override
    public void visit(Structure n) {

    }

    @Override
    public void visit(Field n) {

    }

    @Override
    public void visit(Decl_var n) {
        vars.put(n.name,currentRegister);
    }

    @Override
    public void visit(Expr n) {

    }

    @Override
    public void visit(Econst n) {
        RTL instr = new Rconst(n.i, currentRegister, entry);
        entry = graph.add(instr);
    }

    @Override
    public void visit(Eaccess_local n) {
        entry = graph.add(new Rmbinop(Mbinop.Mmov, vars.get(n.i), currentRegister, entry));
    }

    @Override
    public void visit(Eaccess_field n) {
        Register realcurrentRegister = currentRegister;
        currentRegister = new Register();
        entry = graph.add(new Rload(currentRegister, n.f.field_position * 8, realcurrentRegister, entry));
        n.e.accept(this);
    }

    @Override
    public void visit(Eassign_local n) {
        // On ne garde pas le résultat si on en a pas besoin
        if (currentRegister != null)
            entry = graph.add(new Rmbinop(Mbinop.Mmov, vars.get(n.i), currentRegister, entry));
        currentRegister = vars.get(n.i);
        n.e.accept(this);
    }

    @Override
    public void visit(Eassign_field n) {
        if (currentRegister == null)
            currentRegister = new Register();
        Register realcurrentRegister = currentRegister;
        Register variable = new Register();
        entry = graph.add(new Rstore(realcurrentRegister, variable, n.f.field_position * 8, entry));
        currentRegister = variable;
        n.e1.accept(this);
        currentRegister = realcurrentRegister;
        n.e2.accept(this);
    }

    @Override
    public void visit(Eunop n) {
        switch (n.u) {
      case Uneg:
        Register r = new Register();
        entry = graph.add(new Rmbinop(Mbinop.Msub, r, currentRegister, entry));
        entry = graph.add(new Rconst(0, r, entry));
        currentRegister = r;
        n.e.accept(this);
        break;
      case Unot:
        entry = graph.add(new Rmunop(new Msetei(0), currentRegister, entry));
        n.e.accept(this);
        break;
    }
    }

    @Override
    public void visit(Ebinop n) {
        if (n.b == Binop.Band || n.b == Binop.Bor) {
            Register realcurrentRegister = currentRegister;
            entry = graph.add(new Rmunop(new Msetnei(0), currentRegister, entry));
            Label oldEntry = entry;
            n.e2.accept(this);
            currentRegister = realcurrentRegister;
            Label secondLabel = entry;
            Mubranch op = n.b == Binop.Band ? new Mjz() : new Mjnz();
            entry = graph.add(new Rmubranch(op, currentRegister, oldEntry, secondLabel));
            n.e1.accept(this);
        } else {
            Mbinop mbinop = null;
            switch (n.b) {
                case Beq:
                    mbinop = Mbinop.Msete;
                    break;
                case Bneq:
                    mbinop = Mbinop.Msetne;
                    break;
                case Blt:
                    mbinop = Mbinop.Msetl;
                    break;
                case Ble:
                    mbinop = Mbinop.Msetle;
                    break;
                case Bgt:
                    mbinop = Mbinop.Msetg;
                    break;
                case Bge:
                    mbinop = Mbinop.Msetge;
                    break;
                case Badd:
                    mbinop = Mbinop.Madd;
                    break;
                case Bsub:
                    mbinop = Mbinop.Msub;
                    break;
                case Bmul:
                    mbinop = Mbinop.Mmul;
                    break;
                case Bdiv:
                    mbinop = Mbinop.Mdiv;
                    break;
            }
            Register r1 = new Register();
            Register r2 = currentRegister;
            entry = graph.add(new Rmbinop(mbinop, r1, r2, entry));
            currentRegister = r1;
            n.e2.accept(this);
            currentRegister = r2;
            n.e1.accept(this);
        }
    }

    @Override
    public void visit(Ecall n) {
        Register realcurrentRegister = currentRegister;
        List<Register> args = new LinkedList<>();

        entry = graph.add(new Rcall(realcurrentRegister, n.i, args, entry));

        for (Expr e : n.el) {
            currentRegister = new Register();
            args.add(currentRegister);
            e.accept(this);
        }
    }

    @Override
    public void visit(Esizeof n) {
        RTL instr = new Rconst(n.s.size * 8, currentRegister, entry);
        entry = graph.add(instr);
    }

    @Override
    public void visit(Sskip n) {
        // THIS IS A NO-OP
    }

    @Override
    public void visit(Sexpr n) {
        currentRegister = null;
        n.e.accept(this);
    }


    @Override
    public void visit(Sif n) {
        Label l = entry;
        n.s2.accept(this);
        Label lTrue = entry;
        entry = l;
        n.s1.accept(this);
        Label lFalse = entry;
        Register r1 = new Register();
        entry = graph.add(new Rmubranch(new Mjnz(), r1, lTrue, lFalse));
        currentRegister = r1;
        n.e.accept(this);
    }

    @Override
    public void visit(Swhile n) {
        Label ld = entry;
        Label lg = new Label();
        entry = lg;
        n.s.accept(this);
        entry = graph.add(new Rmubranch(new Mjnz(), currentRegister, entry, ld));
        n.e.accept(this);
        graph.graph.put(lg, new Rgoto(entry));
    }

    @Override
    public void visit(Sblock n) {
        for (int i = 0; i < n.dl.size(); i++) {
            n.dl.get(i).accept(this);
        }
        for (int i = n.sl.size() - 1; i >= 0; i--) {
            n.sl.get(i).accept(this);
        }
    }

    @Override
    public void visit(Sreturn n) {
        currentRegister = fun.result;
        entry = fun.exit;
        n.e.accept(this);
    }

    @Override
    public void visit(Decl_fun n) {
        fun = new RTLfun(n.fun_name);
        fun.result = new Register();
        fun.exit = new Label();
        for (Decl_var arg : n.fun_formals)
            fun.formals.add(arg.register);
        graph = new RTLgraph();
        entry = fun.exit;
        n.fun_body.accept(this);
        fun.body = graph;
        fun.entry = entry;
    }

    @Override
    public void visit(File n) {
        file = new RTLfile();
        for (Decl_fun f : n.funs) {
            f.accept(this);
            file.funs.add(fun);
        }
    }

    RTLfile translate(File n) {
        n.accept(this);
        return file;
    }
}
