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

package solver.constraints.binary;

import choco.kernel.ESat;
import choco.kernel.common.util.tools.ArrayUtils;
import solver.Solver;
import solver.constraints.IntConstraint;
import solver.constraints.propagators.PropagatorPriority;
import solver.constraints.propagators.binary.PropElement;
import solver.variables.IntVar;

/**
 * VALUE = TABLE[INDEX]
 * <p/>
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 20 sept. 2010
 */
public class Element extends IntConstraint<IntVar> {

    final int[] values;
    final int offset;

    public Element(IntVar v0, int[] values, IntVar v1, int offset, Solver solver) {
        this(v0, values, v1, offset, solver, _DEFAULT_THRESHOLD);
    }

    public Element(IntVar v0, int[] values, IntVar v1, int offset, Solver solver,
                   PropagatorPriority threshold) {
        super(ArrayUtils.toArray(v0, v1), solver, threshold);
        this.values = values;
        this.offset = offset;
        setPropagators(new PropElement(vars[0], values, vars[1], offset, solver, this));
    }

    public Element(IntVar v0, int[] values, IntVar v1, Solver solver) {
        this(v0, values, v1, 0, solver);
    }


    @Override
    public ESat isSatisfied(int[] tuple) {
        return ESat.eval(
                !(tuple[0] - this.offset >= values.length || tuple[0] - this.offset < 0)
                        && this.values[tuple[0] - this.offset] == tuple[1]
        );
    }
}
