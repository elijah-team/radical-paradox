package tripleo.elijah;

import org.junit.Test;
import tripleo.elijah.stages.generate.fake.*;

import static org.junit.Assert.assertEquals;
import static tripleo.elijah_fluffy.util.Helpers.List_of;

public class Tfact1_main2Test {

	@Test
	public void z100_main_fn_hdr() {
		final EL_Hdr eh = new EL_Hdr();

		eh.rt(new el_type_NoneType());
		eh.declaring(new el_genClass("Main"));
		eh.enclosing(new el_outClass("Main"));
		eh.name(new el_name("main"));

		eh.writename(new el_outName("main"));

		final el_Arg arg1 = new el_Arg(new el_genClass("Main"), new el_outName("C"));
		eh.args(List_of(arg1));

		final C_FnHdr cf = new C_FnHdr(eh);

		assertEquals("void", cf.returnType());
		assertEquals("z100main", cf.fnName());
		assertEquals("Z100 *", cf.args(0).theType());
		assertEquals("C", cf.args(0).theName());
	}

	@Test
	public void z100_main_fn_hdr2() {
		final EL_Hdr eh = new EL_Hdr();

		eh.rt(new el_type_NoneType());
		eh.declaring(new el_genClass("Main"));
		eh.enclosing(new el_outClass("Main"));
		eh.name(new el_name("main"));

		eh.writename(new el_outName("main"));

		final el_Arg arg1 = new el_Arg(new el_genClass("Main"), new el_outName("C"));
		eh.args(List_of(arg1));

		final C_FnHdr cf = new C_FnHdr(eh);

		assertEquals("void", cf.returnType());
		assertEquals("z100main", cf.fnName());
		assertEquals("Z100 *", cf.args(0).theType());
		assertEquals("C", cf.args(0).theName());
	}

}
