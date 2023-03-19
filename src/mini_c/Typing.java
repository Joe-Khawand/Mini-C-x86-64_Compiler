package mini_c;
import java.util.HashMap;
import java.util.LinkedList;

public class Typing implements Pvisitor {

    static class Env extends HashMap<String, Decl_var> {}
	private LinkedList<HashMap<String, Pdeclvar>> vars;
    private HashMap<String, Decl_fun> funs = new HashMap<>();
    private HashMap<String, Structure> structs = new HashMap<>();
    private String filename;
    Typ returnTyp = null;


	// le résultat du typage sera mis dans cette variable
	private File file;
	// et renvoyé par cette fonction
	File getFile() {
		if (file == null)
			throw new Error("typing not yet done!");
		return file;
	}
	
    public Typing(String filename) {
        this.filename = filename;

        Decl_var putcharArg = new Decl_var(new Tint(), "c");
        Decl_fun putchar = new Decl_fun(new Tint(), "putchar", new LinkedList<>(Collections.singletonList(putcharArg)), new Sskip());
        funs.put(putchar.fun_name, putchar);

        Decl_var sbrkArg = new Decl_var(new Tint(), "n");
        Decl_fun sbrk = new Decl_fun(new Tvoidstar(), "sbrk", new LinkedList<>(Collections.singletonList(sbrkArg)), new Sskip());
        funs.put(sbrk.fun_name, sbrk);
    }


	// il faut compléter le visiteur ci-dessous pour réaliser le typage

	/* types */
	@Override
	public void visit(Pfile n) {
		// TODO Auto-generated method stub
		boolean mainIsPresent = false;
        file = new File(new LinkedList<>());
        for (Pdecl d : n.l) {
            d.accept(this);
            if (d.decl_fun != null && "main".equals(d.decl_fun.fun_name))
                mainIsPresent = true;
        }
        if (!mainIsPresent)
            throw FunctionTypeError.undefinedFunction("main", new Loc(0, 0), filename);
        file.funs = new LinkedList<>(funs.values());
	}

	@Override
	public void visit(PTint n) {
		// TODO Auto-generated method stub
		n.typ = new Tint();
	}

	@Override
	public void visit(PTstruct n) {
		// TODO Auto-generated method stub
		if (!structs.containsKey(n.id))
            throw StructureTypeError.undefinedStructure(n.id, n.loc, filename);
        n.typ = new Tstructp(structs.get(n.id));

	}

	/* expressions */
	@Override
	public void visit(Pint n) {
		// TODO Auto-generated method stub
		this.expr = new Econst(n.n);
        if (n.n != 0) {
            this.expr.typ = new Tint();
        }
        else {
            this.expr.typ = new Ttypenull();
        }
	}

	@Override
	public void visit(Pident n) {
		// TODO Auto-generated method stub
		Iterator<Env> it = vars.descendingIterator();
        Env env;
        Decl_var v = null;
        while (it.hasNext()) {
            env = it.next();
            if (env.containsKey(n.id)) {
                v = env.get(n.id);
                break;
            }
        }
        if (v == null)
            throw IdentTypeError.variableNotFound(n.id, n.loc, filename);
        n.expr = new Eaccess_local(v);
        n.expr.typ = v.t;
	}

	@Override
	public void visit(Punop n) {
		// TODO Auto-generated method stub
		n.e1.accept(this);
        if (n.op == Unop.Uneg && !n.e1.expr.typ.equals(new Tint())) {
            throw new UnopTypeError(n.op, n.e1.expr.typ, new Tint(), n.loc, filename);
        }
        n.expr = new Eunop(n.op, n.e1.expr);
        n.expr.typ = new Tint();
	}

	@Override
	public void visit(Passign n) {
		// TODO Auto-generated method stub
		n.e1.accept(this);
        Expr e1 = n.e1.expr;
        n.e2.accept(this);
        Expr e2 = n.e2.expr;
        if (e1 == null || e1.typ == null)
            throw new TypingNotDone(n.e1.loc, filename);
        if (e2 == null || e2.typ == null)
            throw new TypingNotDone(n.e2.loc, filename);
        if (!e1.typ.equals(e2.typ)) {
            throw AffectationError.incorrectTypes(e1.typ, e2.typ, n.loc, filename);
        }
        if (e1 instanceof Eaccess_local && n.e1 instanceof Pident)
            n.expr = new Eassign_local(((Eaccess_local) e1).v, e2);
        else {
            assert e1 instanceof Eaccess_field;
            Eaccess_field lvalue = (Eaccess_field) e1;
            n.expr = new Eassign_field(lvalue.e, lvalue.f, e2);
        }
        n.expr.typ = e1.typ;
	}

	@Override
	public void visit(Pbinop n) {
		// TODO Auto-generated method stub
		n.e1.accept(this);
        Expr e1 = n.e1.expr;
        n.e2.accept(this);
        Expr e2 = n.e2.expr;
        if (e1 == null || e2 == null)
            throw new TypingNotDone(n.loc, filename);
        switch (n.op) {
		case Beq:  
        case Bneq: 
        case Blt:  
        case Ble:  
        case Bgt:  
        case Bge:
            if (!e1.typ.equals(e2.typ))
                throw BinopTypeError.sameTypeRequired(n.op, e1.typ, e2.typ, n.loc, filename);
            break;
        case Badd: 
        case Bsub: 
        case Bmul: 
        case Bdiv:
            if (!(new Tint()).equals(e1.typ))
                throw new BinopTypeError(n.op, new Tint(), e1.typ, n.loc, filename);
            if (!(new Tint()).equals(e2.typ))
                throw new BinopTypeError(n.op, new Tint(), e2.typ, n.loc, filename);
            break; 
        case Band: 
        case Bor: 
        default:                         
            break;
        }
        n.expr = new Ebinop(n.op, e1, e2);
        n.expr.typ = new Tint();
	}

	@Override
	public void visit(Parrow n) {
		// TODO Auto-generated method stub
        n.e.accept(this);
        if (n.e.exp == null || n.e.exp.typ == null)
            throw new TypingNotDone(n.e.loc, filename);
        if (!(n.e.exp.typ instanceof Tstructp))
            throw AccessError.badExpression(n.e.exp.typ, n.loc, filename);
        Tstructp s = (Tstructp) n.e.exp.typ;
        if (!s.s.fields.containsKey(n.f))
            throw AccessError.noSuchField(s.s, n.f, n.loc, filename);
        Field f = s.fields.get(n.f);
        n.expr = new Eaccess_field(e, f);
        n.expr.typ = f.typ;
	}

	@Override
	public void visit(Pcall n) {
		// TODO Auto-generated method stub
		if (!funs.containsKey(n.f))
            throw FunctionTypeError.undefinedFunction(n.f, n.loc, filename);
        Decl_fun f = funs.get(n.f);
        LinkedList<Expr> args = new LinkedList<>();
        if (f.fun_formals.size() != n.l.size())
            throw FunctionTypeError.wrongArgumentNumber(n.f, f.fun_formals.size(), n.l.size(), n.loc, filename);
        for (int i = 0; i < f.fun_formals.size(); i++) {
            Pexpr a = n.l.get(i);
            a.accept(this);

            if (f.fun_formals.get(i).t.equals(a.expr.typ))
                args.addLast(a.expr);
            else {
                System.out.println(f.toString());
                throw FunctionTypeError.badArgumentType(n.f, i, f.fun_formals.get(i).t, a.expr.typ, a.loc, filename);
            }
        }
        n.expr = new Ecall(n.f, args);
        n.expr.typ = f.fun_typ;
	}

	@Override
	public void visit(Psizeof n) {
		// TODO Auto-generated method stub
		if (!structs.containsKey(n.id))
            throw StructureTypeError.undefinedStructure(n.id, n.loc, filename);
        n.expr = new Esizeof(structs.get(n.id));
        n.expr.typ = new Tint();

	}

	/* instructions */
	@Override
	public void visit(Pskip n) {
		// TODO Auto-generated method stub
		n.stmt = new Sskip();
        n.stmt.terminating = false;
	}

	@Override
	public void visit(Peval n) {
		// TODO Auto-generated method stub
		n.e.accept(this);
        n.stmt = new Sexpr(n.e.expr);
        n.stmt.terminating = false;
	}

	@Override
	public void visit(Pif n) {
		// TODO Auto-generated method stub
		n.e.accept(this);
        n.s1.accept(this);
        n.s2.accept(this);
        n.stmt = new Sif(n.e.expr, n.s1.stmt, n.s2.stmt);
        n.stmt.terminating = n.s1.stmt.terminating && n.s2.stmt.terminating;
	}

	@Override
	public void visit(Pwhile n) {
		// TODO Auto-generated method stub
		n.e.accept(this);
        n.s1.accept(this);
        n.stmt = new Swhile(n.e.expr, n.s1.stmt);
        n.stmt.terminating = false;
	}

	@Override
	public void visit(Pbloc n) {
		// TODO Auto-generated method stub
		Env locals = new Env();
        for (Pdeclvar dvar : pbloc.vl) {
            dvar.typ.accept(this);
            if (locals.containsKey(dvar.id))
                throw new RedefinitionError("variable", dvar.id, dvar.loc, filename);
            locals.put(dvar.id, new Decl_var(dvar.typ.typ, dvar.id));
        }
        LinkedList<Stmt> instructions = new LinkedList<>();
        boolean terminating = false;
        vars.addLast(locals);
        for (Pstmt s : pbloc.sl) {
            s.accept(this);
            instructions.add(s.stmt);
            terminating = terminating || s.stmt.terminating;
        }
        vars.removeLast();
        pbloc.stmt = new Sblock(new LinkedList<>(locals.values()), instructions);
        pbloc.stmt.terminating = terminating;
	}

	@Override
	public void visit(Preturn n) {
		// TODO Auto-generated method stub
		n.e.accept(this);
        if (!n.e.expr.typ.equals(returnTyp))
            throw new ReturnTypeError(returnTyp, n.e.expr.typ, n.loc, filename);
        n.stmt = new Sreturn(n.e.expr);
        n.stmt.terminating = true;
	}

	/* others */
	@Override
	public void visit(Pstruct n) {
		// TODO Auto-generated method stub
		if (structs.containsKey(n.s))
            throw new RedefinitionError("structure", n.s, new Loc(0, 0), filename);
        Structure s = new Structure(n.s);
        structs.put(n.s, s);
        HashMap<String, Field> fields = new HashMap<>();
        for (Pdeclvar dvar : n.fl) {
            dvar.typ.accept(this);
            if (fields.containsKey(dvar.id))
                throw new RedefinitionError("field", dvar.id, dvar.loc, filename);
            Field field = new Field(dvar.id, dvar.typ.typ);
            field.pos = fields.size();
            fields.put(dvar.id, field);
        }
        s.fields = fields;
        s.size = s.fields.size();
	}

	@Override
	public void visit(Pfun n) {
		// TODO Auto-generated method stub
		if (funs.containsKey(pfun.s))
            throw new RedefinitionError("function", pfun.s, pfun.loc, filename);
        pfun.ty.accept(this);
        Env arguments = new Env();
        LinkedList<Decl_var> args = new LinkedList<>();
        for (Pdeclvar dvar : pfun.pl) {
            dvar.typ.accept(this);
            if (arguments.containsKey(dvar.id))
                throw new RedefinitionError("argument", dvar.id, dvar.loc, filename);
            Decl_var arg = new Decl_var(dvar.typ.typ, dvar.id);
            arguments.put(dvar.id, arg);
            args.addLast(arg);
        }
        pfun.decl_fun = new Decl_fun(pfun.ty.typ, pfun.s, args, null);
        funs.put(pfun.s, pfun.decl_fun);
        returnTyp = pfun.ty.typ;
        vars.addLast(arguments);
        pfun.b.accept(this);
        vars.removeLast();
        if (!pfun.b.stmt.terminating) {
            System.err.println(String.format("WARNING: Reached end of function %s at %s without finding a return statement", pfun.s, pfun.loc));
        }
        pfun.decl_fun.fun_body = pfun.b.stmt;
	}

}
