package mini_c;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class Arcs {
    Set<Register> preferences = new HashSet<>();
    Set<Register> interferences = new HashSet<>();

    static Arcs emptyArcs = new Arcs();
}

public class Interference {
    Map<Register, Arcs> graph = new HashMap<>();

    Interference(Liveness l) {
        buildPreferences(l);
        buildInterferences(l);
    }

    private void initGraph(Register r) {
        if (!graph.containsKey(r))
            graph.put(r, new Arcs());
    }

    private void addPreferenceArc(Register r1, Register r2) {
        initGraph(r1);
        initGraph(r2);
        graph.get(r1).preferences.add(r2);
        graph.get(r2).preferences.add(r1);
    }

    private void buildPreferences(Liveness l) {
        for (Liveness.LiveInfo li : l.info.values()) {
            if (li.instr instanceof ERmbinop) {
                ERmbinop instr = (ERmbinop) li.instr;
                if (instr.m == Mbinop.Mmov && !instr.r2.equals(instr.r1))
                    addPreferenceArc(instr.r1, instr.r2);
            }
        }
    }

    private void addInterferenceArc(Register r1, Register r2) {
        initGraph(r1);
        initGraph(r2);
        graph.get(r1).interferences.add(r2);
        graph.get(r2).interferences.add(r1);
    }

    private void buildInterferences(Liveness lg) {
        for (Liveness.LiveInfo li : lg.info.values()) {
            if (li.instr instanceof ERmbinop && ((ERmbinop) li.instr).m == Mbinop.Mmov) {
                ERmbinop instr = (ERmbinop) li.instr;
                for (Register wi : li.outs)
                    if (!instr.r2.equals(wi) && !instr.r1.equals(wi))
                        addInterferenceArc(instr.r2, wi);
            } else
                for (Register v : li.defs)
                    for (Register wi : li.outs)
                        if (!v.equals(wi))
                            addInterferenceArc(v, wi);
        }
    }

    void print() {
        System.out.println("interference:");
        for (Register r: graph.keySet()) {
            Arcs a = graph.get(r);
            System.out.println("  " + r + " pref=" + a.preferences + " intf=" + a.interferences);
        }
    }
}
