package mini_c;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;

public class Typing implements Pvisitor {

    private HashMap<String, Decl_fun> funs = new HashMap<>();
    private HashMap<String, Structure> structs = new HashMap<>();
    private LinkedList<Decl_fun> Linkedfuns = new LinkedList<>();
    private LinkedList<HashMap<String,Typ>> bloc_variables = new LinkedList<>();
    Typ returnTyp = null;

	// le résultat du typage sera mis dans cette variable
	private File file;
	// et renvoyé par cette fonction
	File getFile() {
		if (file == null)
			throw new Error("typing not yet done!");
		return file;
	}


	// il faut compléter le visiteur ci-dessous pour réaliser le typage

	/* types */
	@Override
	public void visit(Pfile n) {
		// TODO Auto-generated method stub
		LinkedList<Decl_var> putcharVars = new LinkedList<Decl_var>();
		putcharVars.add(new Decl_var(new Tint(), ""));
		Decl_fun putchar = new Decl_fun(new Tint(), "putchar", putcharVars, null);
		funs.put("putchar", putchar);

		LinkedList<Decl_var> mallocVars = new LinkedList<Decl_var>();
		mallocVars.add(new Decl_var(new Tint(), ""));
		Decl_fun malloc = new Decl_fun(new Tvoidstar(), "malloc", mallocVars, null);
		funs.put("malloc", malloc);

        for (Pdecl decl : n.l) {
			decl.accept(this);
		}
        if (!funs.containsKey("main")) {
			throw new Error("Cannot find main function");
		}
		File currentFile = new File(Linkedfuns);
		file = currentFile;
    }

	@Override
	public Typ visit(PTint n) {
		// TODO Auto-generated method stub
		return new Tint();
	}

	@Override
	public Typ visit(PTstruct n) {
		// TODO Auto-generated method stub
		if (!structs.containsKey(n.id))
            throw new Error(n.loc + ": Structure not defined");
        return new Tstructp(structs.get(n.id));

	}

	/* expressions */
	@Override
	public Econst visit(Pint n) {
		// TODO Auto-generated method stub
		Econst e = new Econst(n.n);
        if (n.n != 0) {
            e.typ = new Tint();
        }
        else {
            e.typ = new Ttypenull();
        }
        return e;
	}

	@Override
	public Eaccess_local visit(Pident n) {
		// TODO Auto-generated method stub
        for (int i = bloc_variables.size() - 1; i >=0 ; i--) {
			Typ type = bloc_variables.get(i).get(n.id);
			if (type != null) {
                Eaccess_local e = new Eaccess_local(n.id); 
                e.typ = type;
                return e;
            }
		}
        throw new Error(n.loc + ": " + n.id + " undefined variable");
	}

	@Override
	public Eunop visit(Punop n) {
		// TODO Auto-generated method stub
		Expr e1 = n.e1.accept(this);
        if (n.op == Unop.Uneg && !e1.typ.equals(new Tint())) {
            throw new Error(n.loc + ": Cannot use - with type: " + n.e1.accept(this).typ.toString());
        }
        Eunop e = new Eunop(n.op, e1);
        e.typ = new Tint();
        return e;
	}

	@Override
	public Expr visit(Passign n) {
		// TODO Auto-generated method stub
        Expr e1 = n.e1.accept(this);
        Expr e2 = n.e2.accept(this);
        if (e1 == null || e1.typ == null)
            throw new Error(n.e1.loc + ": Typing not done");
        if (e2 == null || e2.typ == null)
            throw new Error(n.e2.loc + ": Typing not done");
        if (!e1.typ.equals(e2.typ)) {
            throw new Error(n.loc + ": " + e1.typ.toString() + " and " + e2.typ.toString() + "not same types");
        }
        if (e1 instanceof Eaccess_local && n.e1 instanceof Pident) {
            Eassign_local e = new Eassign_local(((Eaccess_local) e1).i, e2);
            e.typ = e1.typ;
            return e;
        } else {
            assert e1 instanceof Eaccess_field;
            Eaccess_field l = (Eaccess_field) e1;
            Eassign_field e = new Eassign_field(l.e, l.f, e2);
            e.typ = l.typ;
            return e;
        }
	}

	@Override
	public Ebinop visit(Pbinop n) {
		// TODO Auto-generated method stub
        Expr e1 = n.e1.accept(this);
        Expr e2 = n.e2.accept(this);
        if (e1 == null || e2 == null)
            throw new Error(n.loc + ": Typing not done");
        switch (n.op) {
		case Beq:  
        case Bneq: 
        case Blt:  
        case Ble:  
        case Bgt:  
        case Bge:
            if (!e1.typ.equals(e2.typ))
                throw new Error( n.loc + ": Type must be compatible");
            break;
        case Badd: 
        case Bsub: 
        case Bmul: 
        case Bdiv:
            if (!(new Tint()).equals(e1.typ))
                throw new Error(n.loc + ": first member must be int");
            if (!(new Tint()).equals(e2.typ))
                throw new Error(n.loc + ": second member must be int");
            break; 
        case Band: 
        case Bor: 
        default:                         
            break;
        }
        Ebinop e = new Ebinop(n.op, e1, e2);
		e.typ = new Tint();
		return e;
	}

	@Override
	public Eaccess_field visit(Parrow n) {
		// TODO Auto-generated method stub
        Expr e = n.e.accept(this);
        if (e == null || e.typ == null)
        throw new Error(n.loc + ": Typing not done");
        if (!(e.typ instanceof Tstructp))
        throw new Error(n.loc + ": bad expression");
        Tstructp s = (Tstructp) e.typ;
        if (!s.s.fields.containsKey(n.f))
            throw new Error(n.loc + ": no such field");
        Field f = s.s.fields.get(n.f);
        Eaccess_field a = new Eaccess_field(e, f);
        a.typ = f.field_typ;
        return a;
	}

	@Override
	public Ecall visit(Pcall n) {
		// TODO Auto-generated method stub
		if (!funs.containsKey(n.f))
        throw new Error(n.loc + ": undefined function");
        Decl_fun f = funs.get(n.f);
        LinkedList<Expr> args = new LinkedList<>();
        if (f.fun_formals.size() != n.l.size())
        throw new Error(n.loc + ": wrong argument number");
        for (int i = 0; i < f.fun_formals.size(); i++) {
            Pexpr a = n.l.get(i);
            Expr b = a.accept(this);

            if (f.fun_formals.get(i).t.equals(b.typ))
                args.addLast(b);
            else {
                System.out.println(f.toString());
                throw new Error(n.loc + ": bad argument type");
            }
        }
        Ecall e = new Ecall(n.f, args);
        e.typ = f.fun_typ;
        return e;
	}

	@Override
	public Esizeof visit(Psizeof n) {
		// TODO Auto-generated method stub
		if (!structs.containsKey(n.id))
            throw new Error(n.loc + ": undefined structure");
        Esizeof e = new Esizeof(structs.get(n.id));
        e.typ = new Tint();
        return e;
	}

	/* instructions */
	@Override
	public Sskip visit(Pskip n) {
		// TODO Auto-generated method stub
		return new Sskip();
	}

	@Override
	public Stmt visit(Peval n) {
		// TODO Auto-generated method stub
		Expr e = n.e.accept(this);
        Sexpr a = new Sexpr(e);
        return a;
	}

	@Override
	public Sif visit(Pif n) {
		// TODO Auto-generated method stub
		Expr e = n.e.accept(this);
        Stmt s1 = n.s1.accept(this);
        Stmt s2 = n.s2.accept(this);
        Sif a = new Sif(e, s1, s2);
        return a;
    }

	@Override
	public Swhile visit(Pwhile n) {
		// TODO Auto-generated method stub
		Expr e = n.e.accept(this);
        Stmt s = n.s1.accept(this);
        Swhile a = new Swhile(e, s);
        return a;
	}

	@Override
	public Sblock visit(Pbloc n) {
		// TODO Auto-generated method stub
        bloc_variables.add(new HashMap<>());
		LinkedList<Decl_var> dl = new LinkedList<Decl_var>();
        for (Pdeclvar dvar : n.vl) {
            Decl_var d = dvar.accept(this);
            if (bloc_variables.getLast().containsKey(d.name)) {
				throw new Error(n.loc + ": redefinition of the variable");
			}
            bloc_variables.getLast().put(d.name, d.t);
            dl.add(d);
        }   
        LinkedList<Stmt> instructions = new LinkedList<>();
        for (Pstmt s : n.sl) {
            Stmt a = s.accept(this);
			instructions.add(a);
        }
        bloc_variables.pollLast();
		Sblock b = new Sblock(dl, instructions);
        return b;
	}

	@Override
	public Sreturn visit(Preturn n) {
		// TODO Auto-generated method stub
		Expr e = n.e.accept(this);
        if (!e.typ.equals(returnTyp))
            throw new Error(n.loc + ": wrong type in return");
        Sreturn s = new Sreturn(e);
        return s;
	}

	/* others */
	@Override
	public Structure visit(Pstruct n) {
		// TODO Auto-generated method stub
		if (structs.containsKey(n.s))
            throw new Error("redefinition of the structure");
        Structure s = new Structure(n.s);
        structs.put(n.s, s);
        HashMap<String, Field> fields = new HashMap<>();
        int p = 0;
        for (Pdeclvar dvar : n.fl) {
            dvar.typ.accept(this);
            if (fields.containsKey(dvar.id))
                throw new Error("redefinition of the field");
            Field field = new Field(dvar.id, dvar.typ.accept(this),p);
            fields.put(dvar.id,field);
            p+=8;
        }
        s.fields = fields;
        s.size = s.fields.size();
        return s;
	}

	@Override
	public void visit(Pfun n) {
		// TODO Auto-generated method stub
		if (funs.containsKey(n.s))
            throw new Error("redefinition of the function");
        bloc_variables.add(new HashMap<>());
        returnTyp = n.ty.accept(this);
        LinkedList<Decl_var> f = new LinkedList<Decl_var>();
        Decl_fun fun = new Decl_fun(returnTyp, n.s, f, null);
		funs.put(fun.fun_name, fun);
        for (Pdeclvar dvar : n.pl) {
            Decl_var d = dvar.accept(this);
            if (bloc_variables.getLast().containsKey(d.name)) {
					throw new Error(n.loc + ": this variable is already declared");
			}
            bloc_variables.getLast().put(dvar.id, dvar.typ.accept(this));
			fun.fun_formals.add(d);
        }
        fun.fun_body = n.b.accept(this);
		Linkedfuns.add(fun);
        bloc_variables.pollLast();
	}

    @Override
	public Decl_var visit(Pdeclvar n) {
		return new Decl_var(n.typ.accept(this), n.id);
	}
}
