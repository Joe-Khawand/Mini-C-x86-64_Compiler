package mini_c;

import java.util.LinkedList;

enum Binop {
	Beq, Bneq, Blt, Ble, Bgt, Bge, Badd, Bsub, Bmul, Bdiv, Band, Bor
}

enum Unop {
	Uneg, Unot
}

// une classe pour les localisations
class Loc {
	final int line, column;

	Loc(int line, int column) {
		this.line = line;
		this.column = column;
	}

	@Override
	public String toString() {
		return "line " + line + ", column " + column;
	}
}

class Pstring {
	String id;
	Loc loc;

	public Pstring(String id, Loc loc) {
		this.id = id;
		this.loc = loc;
	}

	@Override
	public String toString() {
		return this.id;
	}
}

class Pfile {
	LinkedList<Pdecl> l;

	public Pfile(LinkedList<Pdecl> l) {
		super();
		this.l = l;
	}

	void accept(Pvisitor v) {
		v.visit(this);
	}
}

abstract class Pdecl {
	abstract void accept(Pvisitor v);
}

class Pstruct extends Pdecl {
	String s;
	LinkedList<Pdeclvar> fl;

	public Pstruct(String s, LinkedList<Pdeclvar> fl) {
		super();
		this.s = s;
		this.fl = fl;
	}

	void accept(Pvisitor v) {
		v.visit(this);
	}
}

class Pfun extends Pdecl {
	Ptype ty;
	String s;
	LinkedList<Pdeclvar> pl;
	Pbloc b;
	Loc loc;

	public Pfun(Ptype ty, Pstring s, LinkedList<Pdeclvar> pl, Pbloc b) {
		super();
		this.ty = ty;
		this.s = s.id;
		this.loc = s.loc;
		this.pl = pl;
		this.b = b;
	}

	void accept(Pvisitor v) {
		v.visit(this);
	}
}

class Pdeclvar {
	Ptype typ;
	String id;
	Loc loc;

	public Pdeclvar(Ptype typ, Pstring id) {
		super();
		this.typ = typ;
		this.id = id.id;
		this.loc = id.loc;
	}

	Decl_var accept(Pvisitor v) {
		return v.visit(this);
	}
}

/* types */

abstract class Ptype {
	static Ptype ptint = new PTint();

	abstract Typ accept(Pvisitor v);
}

class PTint extends Ptype {
	Typ accept(Pvisitor v) {
		return v.visit(this);
	}
}

class PTstruct extends Ptype {
	String id;
	Loc loc;

	public PTstruct(Pstring id) {
		super();
		this.id = id.id;
		this.loc = id.loc;
	}

	Typ accept(Pvisitor v) {
		return v.visit(this);
	}
}

/* expressions */

abstract class Pexpr {
	Loc loc;

	Pexpr(Loc loc) {
		this.loc = loc;
	}

	abstract Expr accept(Pvisitor v);
}

abstract class Plvalue extends Pexpr {
	Plvalue(Loc loc) {
		super(loc);
	}
}

class Pident extends Plvalue {
	String id;

	public Pident(Pstring id) {
		super(id.loc);
		this.id = id.id;
	}

	Eaccess_local accept(Pvisitor v) {
		return v.visit(this);
	}
}

class Pint extends Pexpr {
	int n;

	public Pint(int n, Loc loc) {
		super(loc);
		this.n = n;
	}

	Econst accept(Pvisitor v) {
		return v.visit(this);
	}

}

class Parrow extends Plvalue {
	Pexpr e;
	String f;

	public Parrow(Pexpr e, String f) {
		super(e.loc);
		this.e = e;
		this.f = f;
	}

	Eaccess_field accept(Pvisitor v) {
		return v.visit(this);
	}
}

class Passign extends Pexpr {
	Plvalue e1;
	Pexpr e2;

	public Passign(Plvalue e1, Pexpr e2) {
		super(e1.loc);
		this.e1 = e1;
		this.e2 = e2;
	}

	Expr accept(Pvisitor v) {
		return v.visit(this);
	}
}

class Pbinop extends Pexpr {
	Binop op;
	Pexpr e1, e2;

	public Pbinop(Binop op, Pexpr e1, Pexpr e2) {
		super(e1.loc);
		this.op = op;
		this.e1 = e1;
		this.e2 = e2;
	}

	Ebinop accept(Pvisitor v) {
		return v.visit(this);
	}
}

class Punop extends Pexpr {
	Unop op;
	Pexpr e1;

	public Punop(Unop op, Pexpr e1, Loc loc) {
		super(loc);
		this.op = op;
		this.e1 = e1;
	}

	Eunop accept(Pvisitor v) {
		return v.visit(this);
	}
}

class Pcall extends Pexpr {
	final String f;
	final LinkedList<Pexpr> l;

	Pcall(Pstring f, LinkedList<Pexpr> l) {
		super(f.loc);
		this.f = f.id;
		this.l = l;
	}

	Ecall accept(Pvisitor v) {
		return v.visit(this);
	}
}

class Psizeof extends Pexpr {
	String id;

	public Psizeof(String id, Loc loc) {
		super(loc);
		this.id = id;
	}

	Esizeof accept(Pvisitor v) {
		return v.visit(this);
	}
}

/* instructions */

abstract class Pstmt {
	Loc loc;

	Pstmt(Loc loc) {
		this.loc = loc;
	}

	abstract Stmt accept(Pvisitor v);

}

class Pbloc extends Pstmt {
	LinkedList<Pdeclvar> vl;
	LinkedList<Pstmt> sl;

	public Pbloc(LinkedList<Pdeclvar> vl, LinkedList<Pstmt> sl, Loc loc) {
		super(loc);
		this.vl = vl;
		this.sl = sl;
	}

	Sblock accept(Pvisitor v) {
		return v.visit(this);
	}

}

class Pskip extends Pstmt {
	Pskip(Loc loc) {
		super(loc);
	}

	Sskip accept(Pvisitor v) {
		return v.visit(this);
	}

}

class Preturn extends Pstmt {
	Pexpr e;

	public Preturn(Pexpr e, Loc loc) {
		super(loc);
		this.e = e;
	}

	Sreturn accept(Pvisitor v) {
		return v.visit(this);
	}

}

class Pif extends Pstmt {
	Pexpr e;
	Pstmt s1, s2;

	public Pif(Pexpr e, Pstmt s1, Pstmt s2, Loc loc) {
		super(loc);
		this.e = e;
		this.s1 = s1;
		this.s2 = s2;
	}

	Sif accept(Pvisitor v) {
		return v.visit(this);
	}

}

class Peval extends Pstmt {
	Pexpr e;

	public Peval(Pexpr e) {
		super(e.loc);
		this.e = e;
	}

	Stmt accept(Pvisitor v) {
		return v.visit(this);
	}
}

class Pwhile extends Pstmt {
	Pexpr e;
	Pstmt s1;

	public Pwhile(Pexpr e, Pstmt s1, Loc loc) {
		super(loc);
		this.e = e;
		this.s1 = s1;
	}

	Swhile accept(Pvisitor v) {
		return v.visit(this);
	}
}

interface Pvisitor {

	public Typ visit(PTint n);

	public Typ visit(PTstruct n);

	public Econst visit(Pint n);

	public Eaccess_local visit(Pident n);

	public Eunop visit(Punop n);

	public Expr visit(Passign n);

	public Ebinop visit(Pbinop n);

	public Eaccess_field visit(Parrow n);

	public Ecall visit(Pcall n);

	public Esizeof visit(Psizeof n);

	public Sskip visit(Pskip n);

	public Stmt visit(Peval n);

	public Sif visit(Pif n);

	public Swhile visit(Pwhile n);

	public Sblock visit(Pbloc n);

	public Sreturn visit(Preturn n);

	public Structure visit(Pstruct n);

	public void visit(Pfun n);

	public void visit(Pfile n);

	public Decl_var visit(Pdeclvar n);
}
