package tripleo.vendor.mal;

import tripleo.vendor.mal.env.*;
import tripleo.vendor.mal.types.*;

import java.io.*;
import java.util.*;

public class stepA_mal {
	// read
	public static MalVal READ(final String str) throws MalThrowable {
		return reader.read_str(str);
	}

	// eval
	public static Boolean starts_with(final MalVal ast, final String sym) {
		// Liskov, forgive me
		if (ast instanceof MalList && !(ast instanceof MalVector) && ((MalList) ast).size() == 2) {
			final MalVal a0 = ((MalList) ast).nth(0);
			return a0 instanceof MalSymbol && ((MalSymbol) a0).getName().equals(sym);
		}
		return false;
	}

	public static MalVal quasiquote(final MalVal ast) {
		if ((ast instanceof MalSymbol || ast instanceof MalHashMap))
			return new MalList(new MalSymbol("quote"), ast);

		if (!(ast instanceof MalList))
			return ast;

		if (starts_with(ast, "unquote"))
			return ((MalList) ast).nth(1);

		MalVal res = new MalList();
		for (Integer i = ((MalList) ast).size() - 1; 0 <= i; i--) {
			final MalVal elt = ((MalList) ast).nth(i);
			if (starts_with(elt, "splice-unquote"))
				res = new MalList(new MalSymbol("concat"), ((MalList) elt).nth(1), res);
			else
				res = new MalList(new MalSymbol("cons"), quasiquote(elt), res);
		}
		if (ast instanceof MalVector)
			res = new MalList(new MalSymbol("vec"), res);
		return res;
	}

	public static Boolean is_macro_call(final MalVal ast, final Env env) throws MalThrowable {
		if (ast instanceof MalList) {
			final MalVal a0 = ((MalList) ast).nth(0);
			if (a0 instanceof MalSymbol && env.find(((MalSymbol) a0)) != null) {
				final MalVal mac = env.get(((MalSymbol) a0));
				return mac instanceof MalFunction && ((MalFunction) mac).isMacro();
			}
		}
		return false;
	}

	public static MalVal macroexpand(MalVal ast, final Env env) throws MalThrowable {
		while (is_macro_call(ast, env)) {
			final MalSymbol a0 = (MalSymbol) ((MalList) ast).nth(0);
			final MalFunction mac = (MalFunction) env.get(a0);
			ast = mac.apply(((MalList) ast).rest());
		}
		return ast;
	}

	public static MalVal eval_ast(final MalVal ast, final Env env) throws MalThrowable {
		if (ast instanceof MalSymbol) {
			return env.get((MalSymbol) ast);
		} else if (ast instanceof final MalList old_lst) {
			final MalList new_lst = ast.list_Q() ? new MalList() : new MalVector();
			for (final MalVal mv : (List<MalVal>) old_lst.value) {
				new_lst.conj_BANG(EVAL(mv, env));
			}
			return new_lst;
		} else if (ast instanceof MalHashMap) {
			final MalHashMap new_hm = new MalHashMap();
			final Iterator it = ((MalHashMap) ast).value.entrySet().iterator();
			while (it.hasNext()) {
				final Map.Entry entry = (Map.Entry) it.next();
				new_hm.value.put(entry.getKey(), EVAL((MalVal) entry.getValue(), env));
			}
			return new_hm;
		} else {
			return ast;
		}
	}

	public static MalVal EVAL(MalVal orig_ast, Env env) throws MalThrowable {
		MalVal a0, a1, a2, a3, res;
		MalList el;

		while (true) {

			// System.out.println("EVAL: " + printer._pr_str(orig_ast, true));
			if (!orig_ast.list_Q()) {
				return eval_ast(orig_ast, env);
			}
			if (((MalList) orig_ast).size() == 0) {
				return orig_ast;
			}

			// apply list
			final MalVal expanded = macroexpand(orig_ast, env);
			if (!expanded.list_Q()) {
				return eval_ast(expanded, env);
			}
			final MalList ast = (MalList) expanded;
			if (ast.size() == 0) {
				return ast;
			}
			a0 = ast.nth(0);
			final String a0sym = a0 instanceof MalSymbol ? ((MalSymbol) a0).getName() : "__<*fn*>__";

			switch (a0sym) {
			case "def!":
				a1 = ast.nth(1);
				a2 = ast.nth(2);
				res = EVAL(a2, env);
				env.set(((MalSymbol) a1), res);
				return res;
			case "let*":
				a1 = ast.nth(1);
				a2 = ast.nth(2);
				MalSymbol key;
				MalVal val;
				final Env let_env = new Env(env);
				for (int i = 0; i < ((MalList) a1).size(); i += 2) {
					key = (MalSymbol) ((MalList) a1).nth(i);
					val = ((MalList) a1).nth(i + 1);
					let_env.set(key, EVAL(val, let_env));
				}
				orig_ast = a2;
				env = let_env;
				break;
			case "quote":
				return ast.nth(1);
			case "quasiquoteexpand":
				return quasiquote(ast.nth(1));
			case "quasiquote":
				orig_ast = quasiquote(ast.nth(1));
				break;
			case "defmacro!":
				a1 = ast.nth(1);
				a2 = ast.nth(2);
				res = EVAL(a2, env);
				res = res.copy();
				((MalFunction) res).setMacro();
				env.set((MalSymbol) a1, res);
				return res;
			case "macroexpand":
				a1 = ast.nth(1);
				return macroexpand(a1, env);
			case "try*":
				try {
					return EVAL(ast.nth(1), env);
				} catch (final Throwable t) {
					if (ast.size() > 2) {
						final MalVal exc;
						a2 = ast.nth(2);
						final MalVal a20 = ((MalList) a2).nth(0);
						if (((MalSymbol) a20).getName().equals("catch*")) {
							if (t instanceof MalException) {
								exc = ((MalException) t).getValue();
							} else {
								final StringWriter sw = new StringWriter();
								t.printStackTrace(new PrintWriter(sw));
								final String tstr = sw.toString();
								exc = new MalString(t.getMessage() + ": " + tstr);
							}
							return EVAL(((MalList) a2).nth(2),
									new Env(env, ((MalList) a2).slice(1, 2), new MalList(exc)));
						}
					}
					throw t;
				}
			case "do":
				eval_ast(ast.slice(1, ast.size() - 1), env);
				orig_ast = ast.nth(ast.size() - 1);
				break;
			case "if":
				a1 = ast.nth(1);
				final MalVal cond = EVAL(a1, env);
				if (cond == types.Nil || cond == types.False) {
					// eval false slot form
					if (ast.size() > 3) {
						orig_ast = ast.nth(3);
					} else {
						return types.Nil;
					}
				} else {
					// eval true slot form
					orig_ast = ast.nth(2);
				}
				break;
			case "fn*":
				final MalList a1f = (MalList) ast.nth(1);
				final MalVal a2f = ast.nth(2);
				final Env cur_env = env;
				return new MalFunction(a2f, env, a1f) {
					public MalVal apply(final MalList args) throws MalThrowable {
						return EVAL(a2f, new Env(cur_env, a1f, args));
					}
				};
			default:
				el = (MalList) eval_ast(ast, env);
				final MalFunction f = (MalFunction) el.nth(0);
				final MalVal fnast = f.getAst();
				if (fnast != null) {
					orig_ast = fnast;
					env = f.genEnv(el.slice(1));
				} else {
					return f.apply(el.rest());
				}
			}

		}
	}

	// print
	public static String PRINT(final MalVal exp) {
		return printer._pr_str(exp, true);
	}

	// repl
	public static MalVal RE(final Env env, final String str) throws MalThrowable {
		return EVAL(READ(str), env);
	}

//	public static void main(final String[] args) throws MalThrowable {
//		final String prompt = "user> ";
//
//		final Env repl_env = new Env(null);
//
//		// core.java: defined using Java
//		for (final String key : mal.core.ns.keySet()) {
//			repl_env.set(new MalSymbol(key), core.ns.get(key));
//		}
//		repl_env.set(new MalSymbol("eval"), new MalFunction() {
//			public MalVal apply(final MalList args) throws MalThrowable {
//				return EVAL(args.nth(0), repl_env);
//			}
//		});
//		final MalList _argv = new MalList();
//		for (Integer i = 1; i < args.length; i++) {
//			_argv.conj_BANG(new MalString(args[i]));
//		}
//		repl_env.set(new MalSymbol("*ARGV*"), _argv);
//
//
//		// core.mal: defined using the language itself
//		RE(repl_env, "(def! *host-language* \"java\")");
//		RE(repl_env, "(def! not (fn* (a) (if a false true)))");
//		RE(repl_env, "(def! load-file (fn* (f) (eval (read-string (str \"(do \" (slurp f) \"\nnil)\")))))");
//		RE(repl_env, "(defmacro! cond (fn* (& xs) (if (> (count xs) 0) (list 'if (first xs) (if (> (count xs) 1) (nth xs 1) (throw \"odd number of forms to cond\")) (cons 'cond (rest (rest xs)))))))");
//
//		Integer fileIdx = 0;
//		if (args.length > 0 && args[0].equals("--raw")) {
//			readline.mode = readline.Mode.JAVA;
//			fileIdx       = 1;
//		}
//		if (args.length > fileIdx) {
//			RE(repl_env, "(load-file \"" + args[fileIdx] + "\")");
//			return;
//		}
//
//		// repl loop
//		RE(repl_env, "(println (str \"Mal [\" *host-language* \"]\"))");
//		while (true) {
//			final String line;
//			try {
//				line = readline.readline(prompt);
//				if (line == null) {
//					continue;
//				}
//			} catch (final readline.EOFException e) {
//				break;
//			} catch (final IOException e) {
//				System.out.println("IOException: " + e.getMessage());
//				break;
//			}
//			try {
//				System.out.println(PRINT(RE(repl_env, line)));
//			} catch (final MalContinue e) {
//				continue;
//			} catch (final MalException e) {
//				System.out.println("Error: " + printer._pr_str(e.getValue(), false));
//				continue;
//			} catch (final MalThrowable t) {
//				System.out.println("Error: " + t.getMessage());
//				continue;
//			} catch (final Throwable t) {
//				System.out.println("Uncaught " + t + ": " + t.getMessage());
//				continue;
//			}
//		}
//	}

	public static class MalEnv2 {
		final Env repl_env = new Env(null);

		{
			for (final String key : tripleo.vendor.mal.core.ns.keySet()) {
				repl_env.set(new MalSymbol(key), tripleo.vendor.mal.core.ns.get(key));
			}
			repl_env.set(new MalSymbol("eval"), new MalFunction() {
				public MalVal apply(final MalList args) throws MalThrowable {
					return EVAL(args.nth(0), repl_env);
				}
			});
		}

		public MalEnv2(String[] args) {
			try {
				if (args == null)
					args = new String[] {};
				init(args);
			} catch (final MalThrowable aE) {
				throw new RuntimeException(aE);
			}
		}

		public void init(final String[] args) throws MalThrowable {
			final MalList _argv = new MalList();
			for (Integer i = 1; i < args.length; i++) {
				_argv.conj_BANG(new MalString(args[i]));
			}
			repl_env.set(new MalSymbol("*ARGV*"), _argv);

			// core.mal: defined using the language itself
			re("(def! *host-language* \"java\")");
			re("(def! not (fn* (a) (if a false true)))");
			re("(def! load-file (fn* (f) (eval (read-string (str \"(do \" (slurp f) \"\nnil)\")))))");
			re("(defmacro! cond (fn* (& xs) (if (> (count xs) 0) (list 'if (first xs) (if (> (count xs) 1) (nth xs 1) (throw \"odd number of forms to cond\")) (cons 'cond (rest (rest xs)))))))");

			int fileIdx = 0;
			if (args.length > 0 && args[0].equals("--raw")) {
				readline.mode = readline.Mode.JAVA;
				fileIdx = 1;
			}
			if (args.length > fileIdx) {
				RE(repl_env, "(load-file \"" + args[fileIdx] + "\")");
			}
		}

		public void re(final String str) throws MalThrowable {
			RE(repl_env, str);
		}

		public void set(final MalSymbol aSymbol, final MalFunction aFunction) {
			repl_env.set(aSymbol, aFunction);
		}
	}
}
