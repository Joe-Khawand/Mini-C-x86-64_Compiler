package mini_c;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.naming.ldap.ExtendedRequest;

public class ToRTL extends EmptyVisitor {

  RTLfile Rfile;
  // Functions translated
  LinkedList<RTLfun> Rfuns = new LinkedList<RTLfun>();

  // Current translated function with its attributes not yet computed
  RTLfun Rfun;
  Label exitLabel;
  Register result;
  RTLgraph graph;

  Register currentRegister;

  HashMap<String, Register> varAsReg = new HashMap<String, Register>();
  HashMap<String, Register> argAsReg = new HashMap<String, Register>();
  boolean isReturn = false;
  Register dstRegister;

  Label nextLabel;

  RTLfile translate(File f) {
    visit(f);
    return Rfile;
  }

  public void visit(File f) {
    Rfile = new RTLfile();
    for (Decl_fun fun : f.funs) {
      visit(fun);
    }
    Rfile.funs = Rfuns;
  }

  public void visit(Decl_fun fun) {
    Rfun = new RTLfun(fun.fun_name);
    exitLabel = new Label();
    Rfun.exit = exitLabel;

    result = new Register();

    graph = new RTLgraph();

    nextLabel = exitLabel;
    for (int i = 0; i < fun.fun_formals.size(); i++) {
      Register r = new Register();
      Rfun.formals.add(r);
      varAsReg.put(fun.fun_formals.get(i).name, r);
    }

    if (fun.fun_body != null) {
      fun.fun_body.accept(this);
    }

    Rfun.result = result;
    Rfun.entry = nextLabel;
    Rfun.body = graph;

    Rfuns.add(Rfun);
  }

  public void visit(Sblock sblock) {
    for (int i = 0; i < sblock.dl.size(); i++) {
      sblock.dl.get(i).accept(this);
    }

    for (int i = sblock.sl.size() - 1; i >= 0; i--) {
      sblock.sl.get(i).accept(this);
    }
  }

  public void visit(Sreturn sreturn) {
    nextLabel = exitLabel;
    currentRegister = result;
    sreturn.e.accept(this);
  }

  public void visit(Econst e) {
    nextLabel = graph.add(new Rconst(e.i, currentRegister, nextLabel));
  }

  public void visit(Eunop e) {
    switch (e.u) {
      case Uneg:
        Register r = new Register();

        nextLabel =
          graph.add(new Rmbinop(Mbinop.Msub, r, currentRegister, nextLabel));

        nextLabel = graph.add(new Rconst(0, r, nextLabel));

        currentRegister = r;
        e.e.accept(this);
        break;
      case Unot:
        nextLabel =
          graph.add(new Rmunop(new Msetei(0), currentRegister, nextLabel));
        e.e.accept(this);
        break;
    }
  }

  public void visit(Ebinop ebinop) {
    boolean band = false;
    boolean bor = false;
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
      case Band:
        band = true;
        break;
      case Bor:
        bor = true;
        break;
    }

    if (band) {
      Label lTrue = graph.add(new Rconst(0, currentRegister, nextLabel));
      Label lFalse = graph.add(new Rconst(1, currentRegister, nextLabel));
      Label le1IsTrue = graph.add(
        new Rmubranch(new Mjz(), currentRegister, lTrue, lFalse)
      );
      nextLabel = le1IsTrue;
      ebinop.e2.accept(this);
      nextLabel =
        graph.add(new Rmubranch(new Mjz(), currentRegister, lTrue, nextLabel));
      ebinop.e1.accept(this);
    } else if (bor) {
      Label lTrue = graph.add(new Rconst(1, currentRegister, nextLabel));
      Label lFalse = graph.add(new Rconst(0, currentRegister, nextLabel));
      Label le1IsFalse = graph.add(
        new Rmubranch(new Mjz(), currentRegister, lFalse, lTrue)
      );
      nextLabel = le1IsFalse;
      ebinop.e2.accept(this);
      nextLabel =
        graph.add(new Rmubranch(new Mjz(), currentRegister, nextLabel, lTrue));
      ebinop.e1.accept(this);
    } else {
      Register r1 = new Register();
      Register r2 = currentRegister;
      nextLabel = graph.add(new Rmbinop(mbinop, r1, r2, nextLabel));
      currentRegister = r1;
      ebinop.e2.accept(this);
      currentRegister = r2;
      ebinop.e1.accept(this);
    }
    //Beq, Bneq, Blt, Ble, Bgt, Bge, Badd, Bsub, Bmul, Bdiv, Band, Bor
  }

  public void visit(Eaccess_field n) {
    Register r1 = new Register();
    nextLabel =
      graph.add(new Rload(r1, n.f.field_position, currentRegister, nextLabel));
    currentRegister = r1;
    n.e.accept(this);
  }

  public void visit(Eaccess_local n) {
    // We try to access the variable from current local variables
    Register r1 = varAsReg.get(n.i);
    // If it is not a local variable then it is a argument
    if (r1 == null) {
      r1 = argAsReg.get(n.i);
    }
    nextLabel =
      graph.add(new Rmbinop(Mbinop.Mmov, r1, currentRegister, nextLabel));
  }

  public void visit(Eassign_field n) {
    Register r1 = new Register();
    nextLabel = graph.add(
        new Rstore(r1, currentRegister, n.f.field_position, nextLabel));
    n.e1.accept(this);
    currentRegister = r1;
    n.e2.accept(this);
  }

  public void visit(Eassign_local n) {
    // We try to get the register of the variable assigned
    Register r1 = varAsReg.get(n.i);

    // If it isn't a variable, it is a argument
    if (r1 == null) {
      r1 = argAsReg.get(n.i);
    }

    nextLabel = graph.add(new Rmbinop(Mbinop.Mmov, currentRegister,r1, nextLabel));
    n.e.accept(this);
  }

  public void visit(Ecall ecall) {
    // We create all the registers that will be used for the arguments in the call
    LinkedList<Register> rl = new LinkedList<Register>();
    for (Expr expr : ecall.el) {
      rl.add(new Register());
    }

    // We create the call instruction
    nextLabel = graph.add(new Rcall(currentRegister, ecall.i, rl, nextLabel));

    // We translate the arguments in RTL and by assigning their values to the corresponding registers in the previous call
    for (int i = 0; i < rl.size(); i++) {
      currentRegister = rl.get(i);
      ecall.el.get(i).accept(this);
    }
  }

  public void visit(Esizeof n) {
    nextLabel = graph.add(new Rconst(n.s.size, currentRegister, nextLabel));
  }

  public void visit(Sexpr n) {
    n.e.accept(this);
  }

 	public void visit(Unop n) {
	}

	public void visit(Binop n) {
	}

	public void visit(String n) {
	}

	public void visit(Tint n) {
	}

	public void visit(Tstructp n) {
	}

	public void visit(Tvoidstar n) {
	}

	public void visit(Ttypenull n) {
	}

	public void visit(Structure n) {
	}

	public void visit(Field n) {
	}

  public void visit(Decl_var var) {
    Register r = new Register();
    Rfun.locals.add(r);
    varAsReg.put(var.name, r);
  }

  public void visit(Expr n) {
    n.accept(this);
	}

	public void visit(Sskip n) {
	}

  public void visit(Sif n) {
      // We create the instructions for computation of s1 and s2 and store the labels in lTrue and lFalse
    Label l = nextLabel;
    n.s2.accept(this);
    Label lTrue = nextLabel;
    nextLabel = l;
    n.s1.accept(this);
    Label lFalse = nextLabel;
    
    Register r1 = new Register();
    nextLabel = graph.add(new Rmubranch(new Mjnz(), r1, lTrue, lFalse));
    currentRegister = r1;
    n.e.accept(this);
	}

  public void visit(Swhile n) {
    Label ld = nextLabel;

    Label lGoto = new Label();
    nextLabel = lGoto;
    n.s.accept(this);

    nextLabel = graph.add(new Rmubranch(new Mjnz(), currentRegister, nextLabel, ld));

    n.e.accept(this);
    
    graph.graph.put(lGoto, new Rgoto(nextLabel));

	}
}
