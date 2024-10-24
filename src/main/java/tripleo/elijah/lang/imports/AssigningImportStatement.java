package tripleo.elijah.lang.imports;

import tripleo.elijah.contexts.ImportContext;
import tripleo.elijah.lang.*;
import tripleo.elijah_fluffy.util.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 8/7/20 2:09 AM
 */
public class AssigningImportStatement extends _BaseImportStatement {
    final OS_Element parent;
    private final List<Part> _parts = new ArrayList<Part>();
    private Context _ctx;

    public AssigningImportStatement(final OS_Element aParent) {
        parent = aParent;
        if (parent instanceof OS_Container) {
            ((OS_Container) parent).add(this);
        } else throw new NotImplementedException();
    }

    private static Qualident identToQualident(final IdentExpression identExpression) {
        final Qualident r = new Qualident();
        r.append(identExpression);
        return r;
    }

    public void addAssigningPart(final IdentExpression aToken, final Qualident aQualident) {
        final Part p = new Part(aToken, aQualident);
        //		p.name = aToken;
        //		p.value = aQualident;
        addPart(p);
    }

    public void addPart(final Part p) {
        _parts.add(p);
    }

    @Override
    public Context getContext() {
        return parent.getContext();
    }

    @Override
    public void setContext(final ImportContext ctx) {
        _ctx = ctx;
    }

    @Override
    public OS_Element getParent() {
        return parent;
    }

    @Override
    public List<Qualident> parts() {
        final List<Qualident> r = new ArrayList<Qualident>();
        for (final Part part : _parts) {
            r.add(identToQualident(part.name));
        }
        return r;
    }

    public static class Part { // public for ImportStatementBuilder
        final IdentExpression name;
        final Qualident value;

        public Part(final IdentExpression i1, final Qualident q1) {
            name = i1;
            value = q1;
        }
    }
}

//
//
//
