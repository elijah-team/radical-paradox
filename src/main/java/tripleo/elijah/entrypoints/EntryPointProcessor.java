package tripleo.elijah.entrypoints;

import org.jetbrains.annotations.*;
import tripleo.elijah.lang.*;
import tripleo.elijah.stages.deduce.*;
import tripleo.elijah.stages.gen_fn.*;
import tripleo.elijah.work.*;

public interface EntryPointProcessor {
	static @NotNull EntryPointProcessor dispatch(final EntryPoint ep, final DeducePhase aDeducePhase, final WorkList aWl, final GenerateFunctions aGenerateFunctions) {
		if (ep instanceof MainClassEntryPoint) {
			return new EPP_MCEP((MainClassEntryPoint) ep, aDeducePhase, aWl, aGenerateFunctions);
		} else if (ep instanceof ArbitraryFunctionEntryPoint) {
			return new EPP_AFEP((ArbitraryFunctionEntryPoint) ep, aDeducePhase, aWl, aGenerateFunctions);
		}

		throw new IllegalStateException();
	}

	void process();

	class EPP_MCEP implements EntryPointProcessor {
		private final MainClassEntryPoint mcep;
		private final DeducePhase deducePhase;
		private final WorkList wl;
		private final GenerateFunctions generateFunctions;

		public EPP_MCEP(final MainClassEntryPoint aEp, final DeducePhase aDeducePhase, final WorkList aWl, final GenerateFunctions aGenerateFunctions) {
			mcep = aEp;
			deducePhase = aDeducePhase;
			wl = aWl;
			generateFunctions = aGenerateFunctions;
		}

		@Override
		public void process() {
			final @NotNull ClassStatement cs = mcep.getKlass();
			final @NotNull FunctionDef    f  = mcep.getMainFunction();
			final ClassInvocation         ci = deducePhase.registerClassInvocation(cs, null);

			assert ci != null;

			final @NotNull WlGenerateClass job = new WlGenerateClass(generateFunctions, ci, deducePhase.generatedClasses);
			wl.addJob(job);

			final @NotNull FunctionInvocation fi = new FunctionInvocation(f, null, ci, deducePhase.generatePhase);
//				fi.setPhase(phase);
			final @NotNull WlGenerateFunction job1 = new WlGenerateFunction(generateFunctions, fi);
			wl.addJob(job1);
		}
	}

	class EPP_AFEP implements EntryPointProcessor {

		private final ArbitraryFunctionEntryPoint afep;
		private final DeducePhase deducePhase;
		private final WorkList wl;
		private final GenerateFunctions generateFunctions;

		public EPP_AFEP(final ArbitraryFunctionEntryPoint aEp, final DeducePhase aDeducePhase, final WorkList aWl, final GenerateFunctions aGenerateFunctions) {
			afep = aEp;
			deducePhase = aDeducePhase;
			wl = aWl;
			generateFunctions = aGenerateFunctions;
		}

		@Override
		public void process() {
			final @NotNull FunctionDef     f  = afep.getFunction();
			@NotNull final ClassInvocation ci = deducePhase.registerClassInvocation((ClassStatement) afep.getParent());

			final WlGenerateClass job = new WlGenerateClass(generateFunctions, ci, deducePhase.generatedClasses);
			wl.addJob(job);

			final @NotNull FunctionInvocation fi = new FunctionInvocation(f, null, ci, deducePhase.generatePhase);
//				fi.setPhase(phase);
			final WlGenerateFunction job1 = new WlGenerateFunction(generateFunctions, fi);
			wl.addJob(job1);
		}
	}
}