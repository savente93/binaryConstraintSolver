import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

public class FCSolver {

    BinaryCSP problem;
    int branchesExplored;
    int arcsRevised;
    Queue<CSPVariable> varQueue;
    Stack<Map<CSPVariable, Set<Integer>>> pruningStack;

    public final Comparator<CSPVariable> SmallestDomainComparator = new Comparator<CSPVariable>() {

        @Override
        public int compare(CSPVariable arg0, CSPVariable arg1) {

            return arg0.getDomain().size() - arg1.getDomain().size();
        }

    };

    // use for static orderings
    public final Comparator<CSPVariable> OrderComparator = new Comparator<CSPVariable>() {

        @Override
        public int compare(CSPVariable arg0, CSPVariable arg1) {

            return arg0.getOrder() - arg1.getOrder();
        }

    };

    public FCSolver(BinaryCSP problem, boolean dynamicOrdering) {

        super();
        this.problem = problem;
        if (dynamicOrdering) {
            varQueue = new PriorityQueue<CSPVariable>(SmallestDomainComparator);
        } else {
            varQueue = new PriorityQueue<CSPVariable>(OrderComparator);
        }

        varQueue.addAll(problem.getVars());
        pruningStack = new Stack<Map<CSPVariable, Set<Integer>>>();
    }

    public void printSolution() {

        StringBuffer result = new StringBuffer();
        result.append("Branches explored: " + branchesExplored + "\n");
        result.append("Arcs Revised: " + arcsRevised + "\n");
        result.append("Solution: \n");
        for (int i = 0; i < problem.getVars().size(); i++) {
            result.append(problem.getVars().get(i).toString() + "\n");
        }
        System.out.println(result);
    }

    public void assign(CSPVariable var, int value) {

        assert !var.isAssigned();
        assert var.getDomain().contains(value);
        var.setValue(value);
        var.setAssigned(true);
    }

    public void unassign(CSPVariable var) {

        var.setAssigned(false);
    }

    public static void main(String[] args) {

        String CSPLocation = args[0];
        String heuristicLocation;
        FCSolver solver;
        BinaryCSPReader reader = new BinaryCSPReader();
        System.out.println(CSPLocation);
        if (args.length != 2) {

            solver = new FCSolver(reader.readBinaryCSP(CSPLocation), true);
            System.out.println("Smallest Domain");
        } else {
            heuristicLocation = args[1];
            solver = new FCSolver(reader.readBinaryCSP(CSPLocation, heuristicLocation), false);
            System.out.println(heuristicLocation);
        }

        solver.solveCurrentProblem();
    }

    public void solveCurrentProblem() {

        if (!forwardChecking()) {
            System.out.println("No solution possible");
        }
    }

    // just return an element, we can get fancy later
    private int selectValFromDomain(CSPVariable var) {

        return var.getDomain().iterator().next();
    }

    // adapted from lecture slides.
    public boolean forwardChecking() {

        if (problem.completeAssignment()) {
            assert problem.isConsistent();
            printSolution();
            return true;

        }
        CSPVariable var = varQueue.peek();
        int val = selectValFromDomain(var);
        return branchFCLeft(var, val) || branchFCRight(var, val);
    }

    public boolean branchFCLeft(CSPVariable var, int val) {

        branchesExplored++;
        assign(var, val);

        if (reviseFutureArcs(var)) {
            varQueue.remove(var);
            if (forwardChecking()) {
                return true;
            }
            varQueue.offer(var);
        }
        unassign(var);
        undoPruning();
        return false;

    }

    public boolean branchFCRight(CSPVariable var, int val) {

        branchesExplored++;
        var.removeFromDomain(val);

        if (!var.getDomain().isEmpty()) {
            if (reviseFutureArcs(var)) {
//            	assert problem.isConsistent();
                if (forwardChecking()) {
                    return true;
                }
            }
            undoPruning();
        }
        restoreValue(var, val);
        return false;
    }

    private void undoPruning() {

        Map<CSPVariable, Set<Integer>> pruned = pruningStack.pop();
        for (CSPVariable future : pruned.keySet()) {
            for (int val : pruned.get(future)) {
                future.addToDomain(val);
            }
        }
    }

    private Set<Integer> revise(BinaryArc arc) {

        Set<Integer> toDelete = new HashSet<Integer>();
        if (arc.getDestination().isAssigned()) {
            return toDelete;
        }
        for (int futureVal : arc.getDestination().getDomain()) {
            if (!arc.isSupported(futureVal)) {
                toDelete.add(futureVal);
            }
        }
        arc.getDestination().removeFromDomain(toDelete);
        if (!toDelete.isEmpty()) {
            arcsRevised++;
        }

        return toDelete;
    } 

    private boolean reviseFutureArcs(CSPVariable currentVar) {

        Map<CSPVariable, Set<Integer>> pruned = new HashMap<CSPVariable, Set<Integer>>();
        boolean consistent = true;
        Collection<BinaryArc> arcsToRevise = problem.getOutgoingArcs(currentVar);
        for (BinaryArc arc : arcsToRevise) {
            CSPVariable futureVar = arc.getDestination();
            Set<Integer> deleted = revise(arc);
            consistent = !futureVar.getDomain().isEmpty();
            if (!pruned.keySet().contains(futureVar)) {
                pruned.put(futureVar, new HashSet<Integer>());
            }
            pruned.get(futureVar).addAll(deleted);
            if (!consistent) {
                pruningStack.push(pruned);
                return false;
            }

        }
        pruningStack.push(pruned);
        return true;

    }

    @Override
    public String toString() {

        return "FCSolver \n" + problem.toString();
    }

    private void restoreValue(CSPVariable var, int val) {

        var.addToDomain(val);

    }
}
