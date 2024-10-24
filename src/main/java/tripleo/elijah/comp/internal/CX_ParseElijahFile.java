package tripleo.elijah.comp.internal;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import tripleo.elijah.Out;
import tripleo.elijah.comp.Compilation;
import tripleo.elijah.comp.IO;
import tripleo.elijah.comp.Operation;
import tripleo.elijah.comp.specs.ElijahCache;
import tripleo.elijah.comp.specs.ElijahSpec;
import tripleo.elijah.lang.OS_Module;
import tripleo.elijah.util.Mode;
import tripleo.elijah_fluffy.util.SimplePrintLoggerToRemoveSoon;
import tripleo.elijjah.ElijjahLexer;
import tripleo.elijjah.ElijjahParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

class CX_ParseElijahFile {

    public static Operation<OS_Module> parseAndCache(
            final ElijahSpec aSpec,
            final ElijahCache aElijahCache,
            final String absolutePath,
            final Compilation compilation) {
        final Operation<OS_Module> calm;
        try {
            calm = parseElijahFile_(aSpec, compilation, new File(absolutePath), compilation);
        } catch (final IOException aE) {
            return Operation.failure(aE);
        }

        if (calm.mode() == Mode.SUCCESS) {
            aElijahCache.put(aSpec, absolutePath, calm.success());
        }

        return calm;
    }

    private static Operation<OS_Module> parseElijahFile_(
            final ElijahSpec spec, final Compilation aCompilation, final File file, final Compilation c)
            throws IOException {
        final IO io = aCompilation.getIO();

        // tree add something

        final String f = spec.f();
        final boolean do_out = spec.do_out();
        final OS_Module R;

        try (final InputStream s = io.readFile(file)) {
            final String absolutePath = file.getCanonicalPath();

            final Operation<OS_Module> om = parseElijahFile(f, s, do_out, c, absolutePath);
            if (om.mode() == Mode.FAILURE) {
                final Exception e = om.failure();
                assert e != null;

                SimplePrintLoggerToRemoveSoon.println_err2(("parser exception: " + e));
                e.printStackTrace(System.err);
                s.close();
                return Operation.failure(e);
            }
            R = om.success();
        }
        return Operation.success(R);
    }

    public static Operation<OS_Module> parseElijahFile(
            final String f,
            final InputStream s,
            final boolean do_out,
            final Compilation compilation,
            final String absolutePath) {
        final ElijjahLexer lexer = new ElijjahLexer(s);
        lexer.setFilename(f);
        final ElijjahParser parser = new ElijjahParser(lexer);
        parser.out = new Out(f, compilation, do_out);
        parser.setFilename(f);
        try {
            parser.program();
        } catch (final RecognitionException | TokenStreamException aE) {
            return Operation.failure(aE);
        }
        final OS_Module module = parser.out.module();
        parser.out = null;

        module.setFileName(absolutePath);
        return Operation.success(module);
    }

    private static Operation<OS_Module> parseElijahFile(final ElijahSpec spec, final Compilation compilation) {
        final String absolutePath;
        try {
            absolutePath = spec.file().getCanonicalPath();
        } catch (IOException e) {
            return Operation.failure(e);
        }
        // final var absolutePath = new File(spec.f()).getAbsolutePath(); // !!
        return parseElijahFile(spec.f(), spec.s(), spec.do_out(), compilation, absolutePath);
    }
}
