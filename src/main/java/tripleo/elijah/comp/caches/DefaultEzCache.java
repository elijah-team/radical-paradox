package tripleo.elijah.comp.caches;

import tripleo.elijah.ci.CompilerInstructions;
import tripleo.elijah.comp.specs.EzCache;
import tripleo.elijah.comp.specs.EzSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class DefaultEzCache implements EzCache {
    final Map<String, CompilerInstructions> fn2ci = new HashMap<String, CompilerInstructions>();

    @Override
    public Optional<CompilerInstructions> get(final String absolutePath) {
        if (fn2ci.containsKey(absolutePath)) {
            return Optional.of(fn2ci.get(absolutePath));
        }

        return Optional.empty();
    }

    @Override
    public void put(final EzSpec aSpec, final String aAbsolutePath, final CompilerInstructions aCompilerInstructions) {
        fn2ci.put(aAbsolutePath, aCompilerInstructions);
    }
}
