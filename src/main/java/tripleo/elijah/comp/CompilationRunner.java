package tripleo.elijah.comp;

import antlr.*;
import org.jetbrains.annotations.*;
import tripleo.elijah.ci.*;
import tripleo.elijah.comp.diagnostic.*;
import tripleo.elijah.comp.queries.*;
import tripleo.elijah.diagnostic.*;
import tripleo.elijah.nextgen.query.*;
import tripleo.elijah.stages.deduce.post_bytecode.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

class CompilationRunner {
	final         Map<String, CompilerInstructions> fn2ci = new HashMap<String, CompilerInstructions>();
	private final Compilation                       compilation;
	private final Compilation.CIS                   cis;
	private final CCI                               cci;

	@Contract(pure = true)
	CompilationRunner(final Compilation aCompilation, final Compilation.CIS a_cis) {
		compilation = aCompilation;
		cis         = a_cis;
		cci         = new CCI(compilation, a_cis);
	}

	void start(final CompilerInstructions ci, final boolean do_out) throws Exception {
		// 0. debugging
		//NotImplementedException.raise();

		// 1. find stdlib
		//   -- question placement
		//   -- ...
		{
			final Operation<CompilerInstructions> x = findStdLib(Compilation.CompilationAlways.defaultPrelude(), compilation);
			if (x.mode() == Mode.FAILURE) {
				compilation.getErrSink().exception(x.failure());
				return;
			}
			logProgress(130, "GEN_LANG: " + x.success().genLang());
		}

		// 2. process the initial
		compilation.use(ci, do_out);

		// 3. do rest
		final ICompilationAccess ca = new DefaultCompilationAccess(compilation);
		final ProcessRecord      pr = new ProcessRecord(ca);
		final RuntimeProcesses   rt = StageToRuntime.get(ca.getStage(), ca, pr);

		rt.run_better();
	}

	/*
	 * Design question:
	 *   - why push and return?
	 *     we only want to check error
	 *     utilize exceptions --> only one usage
	 *     or inline (esp use of Compilation)
	 */
	private @NotNull Operation<CompilerInstructions> findStdLib(final String prelude_name, final @NotNull Compilation c) {
		final ErrSink errSink = c.getErrSink();
		final IO      io      = c.getIO();

		// TODO stdlib path here
		final File local_stdlib = new File("lib_elijjah/lib-" + prelude_name + "/stdlib.ez");
		if (local_stdlib.exists()) {
			try {
				final Operation<CompilerInstructions> oci = realParseEzFile(local_stdlib.getName(), io.readFile(local_stdlib), local_stdlib, c);
				if (oci.mode() == Mode.SUCCESS) {
					c.pushItem(oci.success());
					return oci;
				}
			} catch (final Exception e) {
				return Operation.failure(e);
			}
		}

		return Operation.failure(new Exception() {
			public String message() {
				return "No stdlib found";
			}
		});
	}

	private void logProgress(final int number, final String text) {
		if (number == 130) return;

		System.err.println("" + number + " " + text);
	}

	public Operation<CompilerInstructions> realParseEzFile(final String f,
	                                                       final InputStream s,
	                                                       final @NotNull File file,
	                                                       final Compilation c) {
		final String absolutePath;
		try {
			absolutePath = file.getCanonicalFile().toString();
		} catch (final IOException aE) {
			//throw new RuntimeException(aE);
			return Operation.failure(aE);
		}

		if (fn2ci.containsKey(absolutePath)) { // don't parse twice
			return Operation.success(fn2ci.get(absolutePath));
		}

		try {
			try {
				final Operation<CompilerInstructions> cio = parseEzFile_(f, s);

				if (cio.mode() == Mode.FAILURE) {
					final Exception e = cio.failure();
					assert e != null;

					System.err.println(("parser exception: " + e));
					e.printStackTrace(System.err);
					//s.close();
					return cio;
				}

				final CompilerInstructions R = cio.success();
				R.setFilename(file.toString());
				fn2ci.put(absolutePath, R);
				return cio;
			} catch (final ANTLRException e) {
				System.err.println(("parser exception: " + e));
				e.printStackTrace(System.err);
				return Operation.failure(e);
			}
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (final IOException aE) {
					// TODO return inside finally: is this ok??
					return new Operation<>(null, aE, Mode.FAILURE);
				}
			}
		}
	}

	private Operation<CompilerInstructions> parseEzFile_(final String f, final InputStream s) throws RecognitionException, TokenStreamException {
		final QueryEzFileToModuleParams qp = new QueryEzFileToModuleParams(f, s);
		return new QueryEzFileToModule(qp).calculate();
	}

	public void doFindCIs(final String[] args2) {
		final ErrSink errSink1 = compilation.getErrSink();
		final IO      io       = compilation.getIO();

		// TODO map + "extract"
		find_cis(args2, compilation, errSink1, io);

		cis.almostComplete();
	}

	protected void find_cis(final @NotNull String @NotNull [] args2,
	                        final @NotNull Compilation c,
	                        final @NotNull ErrSink errSink,
	                        final @NotNull IO io) {
		CompilerInstructions ez_file;
		for (int i = 0; i < args2.length; i++) {
			final String  file_name = args2[i];
			final File    f         = new File(file_name);
			final boolean matches2  = Pattern.matches(".+\\.ez$", file_name);
			if (matches2) {
				final ILazyCompilerInstructions ilci = ILazyCompilerInstructions.of(f, c);
				cci.accept(new Maybe<>(ilci, null));
			} else {
				//errSink.reportError("9996 Not an .ez file "+file_name);
				if (f.isDirectory()) {
					final List<CompilerInstructions> ezs = searchEzFiles(f, errSink, io, c);

					switch (ezs.size()) {
						case 0:
							final Diagnostic d_toomany = new TooManyEz_ActuallyNone();
							final Maybe<ILazyCompilerInstructions> m = new Maybe<>(null, d_toomany);
							cci.accept(m);
							break;
						case 1:
							ez_file = ezs.get(0);
							cci.accept(new Maybe<>(ILazyCompilerInstructions.of(ez_file), null));
							break;
						default:
							//final Diagnostic d_toomany = new TooManyEz_UseFirst();
							//add_ci(ezs.get(0));

							// more than 1 (negative is not possible)
							final Diagnostic d_toomany2 = new TooManyEz_BeSpecific();
							final Maybe<ILazyCompilerInstructions> m2 = new Maybe<>(null, d_toomany2);
							cci.accept(m2);
							break;
					}
				} else
					errSink.reportError("9995 Not a directory " + f.getAbsolutePath());
			}
		}
	}

	private @NotNull List<CompilerInstructions> searchEzFiles(final @NotNull File directory, final ErrSink errSink, final IO io, final Compilation c) {
		final List<CompilerInstructions> R = new ArrayList<CompilerInstructions>();
		final FilenameFilter filter = new FilenameFilter() {
			@Override
			public boolean accept(final File file, final String s) {
				final boolean matches2 = Pattern.matches(".+\\.ez$", s);
				return matches2;
			}
		};
		final String[] list = directory.list(filter);
		if (list != null) {
			for (final String file_name : list) {
				try {
					final File                 file   = new File(directory, file_name);
					final CompilerInstructions ezFile = parseEzFile(file, file.toString(), errSink, io, c);
					if (ezFile != null)
						R.add(ezFile);
					else
						errSink.reportError("9995 ezFile is null " + file);
				} catch (final Exception e) {
					errSink.exception(e);
				}
			}
		}
		return R;
	}

	@Nullable CompilerInstructions parseEzFile(final @NotNull File f, final String file_name, final ErrSink errSink, final IO io, final Compilation c) throws Exception {
		final Operation<CompilerInstructions> om = parseEzFile1(f, file_name, errSink, io, c);

		final CompilerInstructions m;

		if (om.mode() == Mode.SUCCESS) {
			m = om.success();

/*
		final String prelude;
		final String xprelude = m.genLang();
		System.err.println("230 " + prelude);
		if (xprelude == null)
			prelude = CompilationAlways.defaultPrelude(); // TODO should be java for eljc
		else
			prelude = null;
*/
		} else {
			m = null;
		}

		return m;
	}

	public @NotNull Operation<CompilerInstructions> parseEzFile1(final @NotNull File f,
	                                                             final String file_name,
	                                                             final ErrSink errSink,
	                                                             final IO io,
	                                                             final Compilation c) {
		System.out.printf("   %s%n", f.getAbsolutePath());
		if (!f.exists()) {
			errSink.reportError(
			  "File doesn't exist " + f.getAbsolutePath());
			return null;
		} else {
			final Operation<CompilerInstructions> om = realParseEzFile(file_name/*, io.readFile(f), f*/, io, c);
			return om;
		}
	}

	private Operation<CompilerInstructions> realParseEzFile(final String f, final @NotNull IO io, final Compilation c) {
		final File file = new File(f);

		try {
			return realParseEzFile(f, io.readFile(file), file, c);
		} catch (final FileNotFoundException aE) {
			return Operation.failure(aE);
		}
	}

}
