package mini_c;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ToRTL implements Visitor {
    RTLfun fun;
    RTLfile file;
    RTLgraph graph;
    Label entry;
    Register target;

    final static boolean OPTIMIZE_CHOICE_OF_INSTR = true;

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

    }

    @Override
    public void visit(Expr n) {

    }

    @Override
    public void visit(Econst n) {
        RTL instr = new Rconst(n.i, target, entry);
        entry = graph.add(instr);
    }

    @Override
    public void visit(Eaccess_local n) {
        entry = graph.add(new Rmbinop(Mbinop.Mmov, n.v.register, target, entry));
    }

    @Override
    public void visit(Eaccess_field n) {
        /* Target code:
        r1 = variable at n.name
        mov i(r1) target
         */
        Register realTarget = target;
        target = new Register();
        entry = graph.add(new Rload(target, n.f.pos * 8, realTarget, entry));
        n.e.accept(this);
    }

    @Override
    public void visit(Eassign_local n) {
        // On ne garde pas le résultat si on en a pas besoin
        if (target != null)
            entry = graph.add(new Rmbinop(Mbinop.Mmov, n.v.register, target, entry));
        target = n.v.register;
        n.e.accept(this);
    }

    @Override
    public void visit(Eassign_field n) {
        /* Target code:
        r1 = variable at n.name
        target = visit(n.e)
        mov target i(r1)
         */

        // Ici, même si on n'a pas besoin du résultat, on est obligé de le stocker dans un pseudo-registre
        if (target == null)
            target = new Register();

        Register realTarget = target;
        Register variable = new Register();
        entry = graph.add(new Rstore(realTarget, variable, n.f.pos * 8, entry));
        target = variable;
        n.e1.accept(this);
        target = realTarget;
        n.e2.accept(this);
    }

    @Override
    public void visit(Eunop n) {
        switch (e.u) {
      case Uneg:
        Register r = new Register();
        entry =
          graph.add(new Rmbinop(Mbinop.Msub, r, currentRegister, entry));
        entry = graph.add(new Rconst(0, r, entry));
        currentRegister = r;
        e.e.accept(this);
        break;
      case Unot:
        entry =
          graph.add(new Rmunop(new Msetei(0), currentRegister, entry));
        e.e.accept(this);
        break;
    }
    }

    @Override
    public void visit(Ebinop n) {
        if (n.b == Binop.Band || n.b == Binop.Bor) {
            /* Targetted code:
            EndEntry:
                Instruction to compute left part into target
                for AND:
                    if target is null, goto oldEntry, else to secondbranchment
                for OR:
                    if target is not null, goto oldEntry, else to Second branchment
            secondLabel:
                Instruction to compute right part into target
            oldEntry:
                result = result & 1
                Rest of the code
            */
            Register realTarget = target;
            entry = graph.add(new Rmunop(new Msetnei(0), target, entry));
            Label oldEntry = entry;
            n.e2.accept(this);
            target = realTarget;
            Label secondLabel = entry;
            Mubranch op = n.b == Binop.Band ? new Mjz() : new Mjnz();
            entry = graph.add(new Rmubranch(op, target, oldEntry, secondLabel));
            n.e1.accept(this);
        } else {
            Mbinop mbinop = null;
            switch (ebinop.b) {
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
            Register r2 = target;
            entry = graph.add(new Rmbinop(mbinop, r1, r2, entry));
            target = r1;
            ebinop.e2.accept(this);
            target = r2;
            ebinop.e1.accept(this);
        }
    }

    @Override
    public void visit(Ecall n) {
        Register realTarget = target;
        List<Register> args = new LinkedList<>();

        entry = graph.add(new Rcall(realTarget, n.i, args, entry));

        for (Expr e : n.el) {
            target = new Register();
            args.add(target);
            e.accept(this);
        }
    }

    @Override
    public void visit(Esizeof n) {
        RTL instr = new Rconst(n.s.size * 8, target, entry);
        entry = graph.add(instr);
    }

    @Override
    public void visit(Sskip n) {
        // THIS IS A NO-OP
    }

    @Override
    public void visit(Sexpr n) {
        // On n'a jamais besoin du résultat d'un Sexpr
        target = null;
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
        target = r1;
        n.e.accept(this);
    }

    @Override
    public void visit(Swhile n) {
        Label ld = entry;
        Label lGoto = new Label();
        entry = lGoto;
        n.s.accept(this);
        entry = graph.add(new Rmubranch(new Mjnz(), target, entry, ld));
        n.e.accept(this);
        graph.graph.put(lGoto, new Rgoto(entry));
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
        target = fun.result;
        entry = fun.exit;
        n.e.accept(this);
    }

    @Override
    public void visit(Decl_fun n) {

        // Allocate registers to function
        fun = new RTLfun(n.fun_name);
        fun.result = new Register();
        fun.exit = new Label();
        for (Decl_var arg : n.fun_formals)
            fun.formals.add(arg.register);

        // Parse body
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
