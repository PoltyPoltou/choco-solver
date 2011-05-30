/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package solver.constraints.propagators.gary.constraintSpecific;

import choco.kernel.ESat;
import choco.kernel.common.util.procedure.IntProcedure;
import choco.kernel.memory.IStateInt;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.propagators.GraphPropagator;
import solver.constraints.propagators.Propagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.requests.GraphRequest;
import solver.requests.IRequest;
import solver.variables.EventType;
import solver.variables.IntVar;
import solver.variables.Variable;
import solver.variables.domain.delta.IntDelta;
import solver.variables.graph.IActiveNodes;
import solver.variables.graph.directedGraph.DirectedGraphVar;

import java.util.LinkedList;

/**
 * @author Jean-Guillaume Fages
 * Ensures that each node in the kernel has exactly NLOOPS loops
 *
 * @param <V>
 */
public class PropNLoops<V extends Variable> extends GraphPropagator<V>{

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	DirectedGraphVar g;
	IntVar nLoops;
	IntProcedure removeProc;
	IntProcedure enforceProc;
	IStateInt nbKerLoop;
	IStateInt nbEnvLoop;
	
	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropNLoops(
			DirectedGraphVar graph, IntVar nL,
			Solver solver,
			Constraint<V, Propagator<V>> constraint,
			PropagatorPriority priority, boolean reactOnPromotion) {
		super((V[]) new Variable[]{graph,nL}, solver, constraint, priority, reactOnPromotion);
		g = graph;
		nLoops = nL;
		removeProc = new RemProc(this);
		enforceProc = new EnfLoop(this);
		nbEnvLoop = environment.makeInt();
		nbKerLoop = environment.makeInt();
	}

	//***********************************************************************************
	// METHODS
	//***********************************************************************************

	@Override
	public void propagate() throws ContradictionException {
		IActiveNodes act = g.getEnvelopGraph().getActiveNodes();
		int ker = 0;
		int env = 0;
		for (int node = act.nextValue(0); node>=0; node = act.nextValue(node+1)) {
			if (g.getEnvelopGraph().arcExists(node, node)){
				env++;
				if (g.getKernelGraph().arcExists(node, node)){
					ker++;
				}
			}			
		}
		nLoops.updateLowerBound(ker, this);
		nLoops.updateUpperBound(g.getEnvelopOrder(), this);
		nLoops.updateUpperBound(env, this);
		int added = 0;
		if(env==nLoops.getLB()){
			for (int node = act.nextValue(0); node>=0; node = act.nextValue(node+1)) {
				if (g.getEnvelopGraph().arcExists(node, node)){
					g.enforceArc(node, node, this);
					added ++;
				}			
			}
		}
		nbEnvLoop.set(env);
		nbKerLoop.set(ker+added);
	}

	@Override
	public void propagateOnRequest(IRequest<V> request, int idxVarInProp, int mask) throws ContradictionException {
		if (request instanceof GraphRequest) {
			GraphRequest gv = (GraphRequest) request;
			if ((mask & EventType.REMOVEARC.mask) != 0){
				IntDelta d = (IntDelta) g.getDelta().getArcRemovalDelta();
				d.forEach(removeProc, gv.fromArcRemoval(), gv.toArcRemoval());
			}
			if ((mask & EventType.ENFORCEARC.mask) != 0){
				IntDelta d = (IntDelta) g.getDelta().getArcEnforcingDelta();
				int nbKer = nbKerLoop.get();
				d.forEach(enforceProc, gv.fromArcEnforcing(), gv.toArcEnforcing());
				if(nbKer<nbKerLoop.get()){
					nLoops.updateLowerBound(nbKerLoop.get(), this);//recently added
					checkAllLoopsFound();
				}
			}
			if ((mask & EventType.REMOVENODE.mask) != 0){
				nLoops.updateUpperBound(g.getEnvelopOrder(), this);
			}
		}
	}

	private void checkAllLoopsFound() throws ContradictionException {
		int loopsInKer = nbKerLoop.get();
		int loopsInEnv = nbEnvLoop.get();
		if(loopsInKer>nLoops.getUB()){
			this.contradiction(g, "too many loops");
		}else{
			IActiveNodes act = g.getEnvelopGraph().getActiveNodes();
			LinkedList<Integer> loopOutOfKer = new LinkedList<Integer>();
			for (int node = act.nextValue(0); node>=0; node = act.nextValue(node+1)) {
				if (!g.getKernelGraph().arcExists(node, node)){
					if (g.getEnvelopGraph().arcExists(node, node)){
						loopOutOfKer.addFirst(node);
					}
				}
			}
			if(loopsInKer==nLoops.getUB()){
				for(int l:loopOutOfKer){
					g.removeArc(l, l, this);
				}
			}else{
				if (loopsInEnv==nLoops.getValue()){
					for(int l:loopOutOfKer){
						g.enforceArc(l, l, this);
					}
				}
			}
		}
	}

	@Override
	public int getPropagationConditions(int vIdx) {
		return EventType.REMOVEARC.mask+EventType.ENFORCEARC.mask+EventType.REMOVENODE.mask;
	}

	@Override
	public ESat isEntailed() {
		return ESat.UNDEFINED;
	}
	
	//***********************************************************************************
	// PROCEDURES
	//***********************************************************************************

	/**
	 * @author Jean-Guillaume Fages
	 * Checks if a loop has been removed
	 */
	private static class RemProc implements IntProcedure {

        private final PropNLoops p;

        public RemProc(PropNLoops p) {
            this.p = p;
        }

        @Override
        public void execute(int i) throws ContradictionException {
    		int n = p.g.getEnvelopGraph().getNbNodes();
        	if (i>=n){
        		int from = i/n-1;
        		int to   = i%n;
        		if (from == to){
        			p.nbEnvLoop.set(p.nbEnvLoop.get()-1);
        			int env = p.nbEnvLoop.get();
        			int ker = p.nbKerLoop.get();
        			IActiveNodes act;
        			p.nLoops.updateUpperBound(env, p);
        			p.nLoops.updateLowerBound(ker, p);
        			if(p.nLoops.getLB() == env && env>ker){
        				act = p.g.getEnvelopGraph().getActiveNodes();
        				for (int node = act.nextValue(0); node>=0; node = act.nextValue(node+1)) {
            				if (p.g.getEnvelopGraph().arcExists(node, node)){
            					p.g.enforceArc(node, node, p);
            				}
            			}
        			}
        		}
        	}else{
        		throw new UnsupportedOperationException();
        	}
        }
    }
	
	/**
	 * @author Jean-Guillaume Fages
	 * Checks if a loop has been enforced
	 */
	private static class EnfLoop implements IntProcedure {

        private final PropNLoops p;

        public EnfLoop(PropNLoops p) {
            this.p = p;
        }

        @Override
        public void execute(int i) throws ContradictionException {
    		int n = p.g.getEnvelopGraph().getNbNodes();
        	if (i>=n){
        		int from = i/n-1;
        		int to   = i%n;
        		if (from == to){
        			p.nbKerLoop.set(p.nbKerLoop.get()+1);
        		}
        	}else{
        		throw new UnsupportedOperationException();
        	}
        }
    }
}
