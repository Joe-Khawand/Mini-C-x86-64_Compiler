package mini_c;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import javax.sql.RowSet;

public class ToERTL extends EmptyRTLVisitor {
    ERTLfile ERfile;
    LinkedList<ERTLfun> ERfuns = new LinkedList<ERTLfun>();
    Set<Register> locals;
    LinkedList<Register> callee_saved = new LinkedList<Register>();
    
    ERTLgraph graph;
    ERTLfun ERfun;


    RTLgraph rtlgraph;

    Label nextLabel;   
    Label currLabel; 
    Label rtlLabel;

    public ERTLfile translate(RTLfile file) {
        file.accept(this);
        return ERfile;
    }
    
    public void visit(RTLfile f) {
        ERfile = new ERTLfile();
        for(RTLfun fun: f.funs) {
            visit(fun);
        }
        System.out.println("ERfuns");
        System.out.println(ERfuns);
        ERfile.funs = ERfuns;
    }

    public void visit(RTLfun fun) {
        ERfun = new ERTLfun(fun.name, fun.formals.size());
        locals = fun.locals;
        callee_saved = new LinkedList<Register>();
        graph = new ERTLgraph();
        
        currLabel = new Label();
        ERfun.entry = currLabel;
        nextLabel = new Label();

        graph.put(currLabel, new ERalloc_frame(nextLabel));
        currLabel = nextLabel;
        nextLabel = new Label();
        
        for (Register calleeSavedRegister: Register.callee_saved) {
            Register r = new Register();
            graph.put(currLabel, new ERmbinop(Mbinop.Mmov, calleeSavedRegister, r, nextLabel));
            callee_saved.add(r);
            currLabel = nextLabel;
            nextLabel = new Label();
            ERfun.locals.add(r);
        }

        nextLabel = fun.entry;
        graph.put(currLabel, new ERgoto(nextLabel));

        this.visit(fun.body);

        currLabel = fun.exit;
        nextLabel = new Label();
        graph.put(currLabel, new ERmbinop(Mbinop.Mmov, fun.result, Register.rax, nextLabel));

        for (int i = Register.callee_saved.size() -1; i>=0; i--) {
            graph.put(currLabel, new ERmbinop(Mbinop.Mmov, callee_saved.pollLast(), Register.callee_saved.get(i), nextLabel));
            currLabel = nextLabel;
            nextLabel = new Label();
        }

        graph.put(currLabel, new ERdelete_frame(nextLabel));
        currLabel = nextLabel;
        nextLabel = new Label();

        graph.put(currLabel, new ERreturn());

        ERfun.body = graph;
        ERfuns.add(ERfun);
    }

    public void visit(Rconst rconst) {
        graph.put(currLabel,new ERconst(rconst.i, rconst.r, rconst.l));
    }

    public void visit(Rload rload) {
        graph.put(currLabel,new ERload(rload.r1, rload.i, rload.r2, rload.l));
    }

    public void visit(Rstore rstore) {
        graph.put(currLabel,new ERstore(rstore.r1, rstore.r2, rstore.i, rstore.l));

    }

    public void visit(Rmunop rmunop) {
        graph.put(currLabel, new ERmunop(rmunop.m, rmunop.r, rmunop.l));
    }

    public void visit(Rmbinop rmbinop) {
        graph.put(currLabel, new ERmbinop(rmbinop.m, rmbinop.r1, rmbinop.r2, rmbinop.l));
    }

    public void visit(Rmubranch ubranch) {
        graph.put(currLabel, new ERmubranch(ubranch.m, ubranch.r, ubranch.l1, ubranch.l2));
    }

    public void visit(Rmbbranch bbranch) {
        graph.put(currLabel, new ERmbbranch(bbranch.m, bbranch.r1, bbranch.r2, bbranch.l1, bbranch.l2));
    }

    public void visit(RTLgraph g) {
        for (Label label: g.graph.keySet()) {
            currLabel = label;
            g.graph.get(currLabel).accept(this);
        }
    }
}
