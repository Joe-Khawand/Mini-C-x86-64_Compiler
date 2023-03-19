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
        if (n.u == Unop.Uneg) {
            Register r1 = new Register();
            entry = graph.add(new Rmbinop(Mbinop.Msub, r1, target, entry));
            entry = graph.add(new Rconst(0, target, entry));
            target = r1;
            n.e.accept(this);
        } else {
            if (OPTIMIZE_CHOICE_OF_INSTR && n.e instanceof Eunop) {
                Eunop subE = (Eunop) n.e;
                if (subE.u == Unop.Uneg) {
                    // Ca ne sert à rien de faire le moins
                    n.e = subE.e;
                } else {
                    // Ca ne sert à rien de faire not not
                    (new Ebinop(Binop.Bneq, new Econst(0), subE.e)).accept(this);
                    return;
                }
            }

            if (OPTIMIZE_CHOICE_OF_INSTR && n.e instanceof Ebinop) {
                Ebinop subE = (Ebinop) n.e;
                switch (subE.b) {
                    case Beq:
                        (new Ebinop(Binop.Bneq, subE.e1, subE.e2)).accept(this);
                        return;
                    case Bneq:
                        (new Ebinop(Binop.Beq, subE.e1, subE.e2)).accept(this);
                        return;
                    case Blt:
                        (new Ebinop(Binop.Bge, subE.e1, subE.e2)).accept(this);
                        return;
                    case Ble:
                        (new Ebinop(Binop.Bgt, subE.e1, subE.e2)).accept(this);
                        return;
                    case Bgt:
                        (new Ebinop(Binop.Ble, subE.e1, subE.e2)).accept(this);
                        return;
                    case Bge:
                        (new Ebinop(Binop.Blt, subE.e1, subE.e2)).accept(this);
                        return;
                    default:
                        break;
                }
            }

            entry = graph.add(new Rmunop(new Msetei(0), target, entry));
            n.e.accept(this);
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
            if (OPTIMIZE_CHOICE_OF_INSTR && (n.b == Binop.Beq || n.b == Binop.Bneq || n.b == Binop.Badd) && (n.e1 instanceof Econst || n.e2 instanceof Econst)) {
                int constant = n.e1 instanceof Econst ? ((Econst) n.e1).i : ((Econst) n.e2).i;
                Munop op;
                switch (n.b) {
                    case Beq:
                        op = new Msetei(constant);
                        break;
                    case Bneq:
                        op = new Msetnei(constant);
                        break;
                    case Badd:
                        op = new Maddi(constant);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + n.b);
                }
                entry = graph.add(new Rmunop(op, target, entry));
                if (n.e1 instanceof Econst)
                    n.e2.accept(this);
                else
                    n.e1.accept(this);
                return;
            }

            Register r1 = new Register();
            entry = graph.add(new Rmbinop(Mbinop.construct(n.b), r1, target, entry));
            n.e1.accept(this);
            target = r1;
            n.e2.accept(this);
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

    RTL parseBiBranch(Mbbranch mbbranch, Label ltrue, Label lfalse, Expr e1, Expr e2) {
        Register r1 = new Register();
        RTL instr = new Rmbbranch(mbbranch, target, r1, ltrue, lfalse);
        entry = graph.add(instr);
        e2.accept(this);
        target = r1;
        e1.accept(this);
        return instr;
    }

    RTL parseBiBranch(Ebinop ebinop, Label ltrue, Label lfalse) {
        switch (ebinop.b) {
            case Ble:
                return parseBiBranch(Mbbranch.Mjle, ltrue, lfalse, ebinop.e1, ebinop.e2);
            case Blt:
                return parseBiBranch(Mbbranch.Mjl, ltrue, lfalse, ebinop.e1, ebinop.e2);
            case Bge:
                return parseBiBranch(Mbbranch.Mjle, ltrue, lfalse, ebinop.e2, ebinop.e1);
            case Bgt:
                return parseBiBranch(Mbbranch.Mjl, ltrue, lfalse, ebinop.e2, ebinop.e1);
            case Beq:
                return parseBiBranch(Mbbranch.Mjeq, ltrue, lfalse, ebinop.e1, ebinop.e2);
            case Bneq:
                return parseBiBranch(Mbbranch.Mjneq, ltrue, lfalse, ebinop.e1, ebinop.e2);
            default:
                throw new ToRTLError("Parse bi branch for branch " + ebinop.b + " not yet implemented.");
        }
    }

    RTL parseBranch(Expr e, Label ltrue, Label lfalse) {
        target = new Register();
        RTL instr;

        if (OPTIMIZE_CHOICE_OF_INSTR && e instanceof Econst) {
            instr = new Rgoto(((Econst) e).i != 0 ? ltrue : lfalse);

        } else if (OPTIMIZE_CHOICE_OF_INSTR && e instanceof Eunop) {
            Mubranch mubranch = ((Eunop) e).u == Unop.Unot ? new Mjz() : new Mjnz();
            instr = new Rmubranch(mubranch, target, ltrue, lfalse);
            e = ((Eunop) e).e;

        } else if (OPTIMIZE_CHOICE_OF_INSTR && e instanceof Ebinop) {
            switch (((Ebinop) e).b) {
                case Blt:
                case Bgt:
                case Bge:
                case Ble:
                case Beq:
                case Bneq:
                    return parseBiBranch((Ebinop) e, ltrue, lfalse);

                default:
                    Mubranch mubranch = new Mjnz();
                    instr = new Rmubranch(mubranch, target, ltrue, lfalse);
                    break;
            }

        } else {
            Mubranch mubranch = new Mjnz();
            instr = new Rmubranch(mubranch, target, ltrue, lfalse);
        }

        entry = graph.add(instr);
        e.accept(this);
        return instr;
    }

    @Override
    public void visit(Sif n) {
        /*
        endEntry:
            Eval if and go to the right label (done with parse branch)
        Lleft:
            Instructions if false
        Ltrue:
            Instructions if true
        oldEntry:
            Instructions after if
         */
        Label oldEntry = entry;
        n.s1.accept(this);
        Label ltrue = entry;
        entry = oldEntry;
        n.s2.accept(this);
        Label lfalse = entry;
        parseBranch(n.e, ltrue, lfalse);
    }

    @Override
    public void visit(Swhile n) {
        /*
        endEntry:
            jmp to ifEntry (ie on fait rien, juste on remet entry à ifEntry à la fin)
        corps:
            Corps de la fonction
        ifEntry:
            Eval if and go to the right label (done with parse branch)
        oldEntry:
            Instructions after while
        */
        Label corps = new Label(); // to destroy after
        RTL ifInstr = parseBranch(n.e, corps, entry);
        Label ifEntry = entry;
        n.s.accept(this);
        Label newCorps = entry;

        if (ifInstr instanceof Rmubranch)
            ((Rmubranch) ifInstr).l1 = newCorps;
        if (ifInstr instanceof Rmbbranch)
            ((Rmbbranch) ifInstr).l1 = newCorps;

        entry = ifEntry;
    }

    @Override
    public void visit(Sblock n) {
        for (Decl_var v : n.dl)
            fun.locals.add(v.register);

        for (Iterator<Stmt> it = n.sl.descendingIterator(); it.hasNext(); )
            it.next().accept(this);
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
