/*
 * Elijjah compiler, copyright Tripleo <oluoluolu+elijah@gmail.com>
 *
 * The contents of this library are released under the LGPL licence v3,
 * the GNU Lesser General Public License text was downloaded from
 * http://www.gnu.org/licenses/lgpl.html from `Version 3, 29 June 2007'
 *
 */
package tripleo.elijah.lang;

/**
 * Created 12/22/20 11:44 PM
 */
public class Precondition {
    private IdentExpression id;
    private IExpression expr;

    public void id(final IdentExpression id) {
        this.id = id;
    }

    public void expr(final IExpression expr) {
        this.expr = expr;
    }
}

//
//
//
