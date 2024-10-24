package tripleo.elijah.lang.imports;

import tripleo.elijah.contexts.ImportContext;
import tripleo.elijah.lang.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 8/7/20 2:09 AM
 */
public class QualifiedImportStatement extends _BaseImportStatement {
    final OS_Element parent;
    private final List<Part> _parts = new ArrayList<Part>();
    private Context _ctx;

    public QualifiedImportStatement(final OS_Element aParent) {
        parent = aParent;
        if (parent instanceof OS_Container) {
            ((OS_Container) parent).add(this);
        } else throw new IllegalStateException();
    }

    public void addSelectivePart(final Qualident aQualident, final IdentList il) {
        final Part p = new Part(aQualident, il);
        //		p.base = aQualident;
        //		p.idents = il;
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
            r.add(part.base);
        }
        return r;
    }

    public static class Part {
        public final Qualident base;
        public final IdentList idents;

        public Part(final Qualident q3, final IdentList il) {
            base = q3;
            idents = il;
        }
    }
}

//
//
//
