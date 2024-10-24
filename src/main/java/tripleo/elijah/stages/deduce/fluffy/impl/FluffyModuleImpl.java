package tripleo.elijah.stages.deduce.fluffy.impl;

import tripleo.elijah.comp.Compilation;
import tripleo.elijah.entrypoints.MainClassEntryPoint;
import tripleo.elijah.lang.ClassItem;
import tripleo.elijah.lang.ClassStatement;
import tripleo.elijah.lang.FunctionDef;
import tripleo.elijah.lang.OS_Module;
import tripleo.elijah.stages.deduce.fluffy.i.FluffyComp;
import tripleo.elijah.stages.deduce.fluffy.i.FluffyLsp;
import tripleo.elijah.stages.deduce.fluffy.i.FluffyMember;
import tripleo.elijah.stages.deduce.fluffy.i.FluffyModule;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FluffyModuleImpl implements FluffyModule {
    private final OS_Module module;
    private final Compilation compilation;

    public FluffyModuleImpl(final OS_Module aModule, final Compilation aCompilation) {
        module = aModule;
        compilation = aCompilation;
    }

    /**
     * If classStatement is a "main class", send to consumer
     *
     * @param classStatement
     * @param ccs
     */
    private static void faep_002(final ClassStatement classStatement, final Consumer<ClassStatement> ccs) {
        final Collection<ClassItem> x = classStatement.findFunction("main");
        final Stream<FunctionDef> found =
                x.stream().filter(FluffyCompImpl::isMainClassEntryPoint).map(x7 -> (FunctionDef) x7);

        //		final int eps = aModule.entryPoints.size();

        found.map(aFunctionDef -> (ClassStatement) aFunctionDef.getParent()).forEach(ccs);

        //		assert aModule.entryPoints.size() == eps || aModule.entryPoints.size() == eps+1; // TODO this will fail one
        // day

        //		tripleo.elijah.util.Stupidity.println2("243 " + entryPoints +" "+ _fileName);
        //		break; // allow for "extend" class
    }

    @Override
    public FluffyLsp lsp() {
        return null;
    }

    @Override
    public String name() {
        return null;
    }

    @Override
    public List<FluffyMember> members() {
        return null;
    }

    @Override
    public void find_multiple_items(final FluffyComp aFc) {
        aFc.find_multiple_items(module);
    }

    @Override
    public void find_all_entry_points() {
        //
        // FIND ALL ENTRY POINTS (should only be one per module)
        //
        final Consumer<ClassStatement> ccs = (x) -> module.entryPoints.add(new MainClassEntryPoint(x));

        module.items.stream()
                .filter(item -> item instanceof ClassStatement)
                .filter(classStatement -> MainClassEntryPoint.isMainClass((ClassStatement) classStatement))
                .forEach(classStatement -> faep_002((ClassStatement) classStatement, ccs));
    }
}
