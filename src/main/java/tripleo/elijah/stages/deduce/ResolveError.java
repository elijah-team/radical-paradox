/*
 * Elijjah compiler, copyright Tripleo <oluoluolu+elijah@gmail.com>
 *
 * The contents of this library are released under the LGPL licence v3,
 * the GNU Lesser General Public License text was downloaded from
 * http://www.gnu.org/licenses/lgpl.html from `Version 3, 29 June 2007'
 *
 */
package tripleo.elijah.stages.deduce;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tripleo.elijah.diagnostic.Diagnostic;
import tripleo.elijah.diagnostic.Locatable;
import tripleo.elijah.lang.IdentExpression;
import tripleo.elijah.lang.LookupResult;
import tripleo.elijah.lang.LookupResultList;
import tripleo.elijah.lang.TypeName;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created 12/26/20 5:08 AM
 */
public class ResolveError extends Exception implements Diagnostic {
    final @org.jetbrains.annotations.Nullable IdentExpression ident;
    private final @org.jetbrains.annotations.Nullable TypeName typeName;
    private final LookupResultList lrl;

    public ResolveError(final TypeName typeName, final LookupResultList lrl) {
        this.typeName = typeName;
        this.lrl = lrl;
        this.ident = null;
    }

    public ResolveError(final IdentExpression aIdent, final LookupResultList aLrl) {
        ident = aIdent;
        lrl = aLrl;
        typeName = null;
    }

    @Override
    public @NotNull String code() {
        return "S1000";
    }

    @Override
    public @NotNull Severity severity() {
        return Severity.ERROR;
    }

    @Override
    public @NotNull Locatable primary() {
        if (typeName == null) {
            return ident;
        } else return typeName;
    }

    @Override
    public @NotNull List<Locatable> secondary() {
        @NotNull
        final Collection<Locatable> x = Collections2.transform(resultsList(), new Function<LookupResult, Locatable>() {
            @Nullable
            @Override
            public Locatable apply(@Nullable final LookupResult input) {
                if (input.getElement() instanceof Locatable) {
                    return (Locatable) input.getElement();
                }
                return null;
            }
        });
        return new ArrayList<Locatable>();
    }

    @Override
    public void report(@NotNull final PrintStream stream) {
        stream.printf("---[%s]---: %s%n", code(), message());
        // linecache.print(primary);
        for (final Locatable sec : secondary()) {
            // linecache.print(sec)
        }
        stream.flush();
    }

    private @NotNull String message() {
        if (resultsList().size() > 1) return "Can't choose between alternatives";
        else return "Can't resolve";
    }

    @NotNull
    public List<LookupResult> resultsList() {
        return lrl.results();
    }
}

//
//
//
