/*
 * Elijjah compiler, copyright Tripleo <oluoluolu+elijah@gmail.com>
 *
 * The contents of this library are released under the LGPL licence v3,
 * the GNU Lesser General Public License text was downloaded from
 * http://www.gnu.org/licenses/lgpl.html from `Version 3, 29 June 2007'
 *
 */
package tripleo.elijah.ci;

import antlr.*;
import org.jetbrains.annotations.*;
import tripleo.elijah.lang.*;
import tripleo.elijah.util.*;

import java.util.*;
import java.util.stream.*;

/**
 * Created 9/6/20 11:20 AM
 */
public class CompilerInstructions {
	private IndexingStatement _idx;
	private GenerateStatement gen;
	public List<LibraryStatementPart> lsps = new ArrayList<LibraryStatementPart>();
	private String filename;
	private String name;

	public IndexingStatement indexingStatement() {
		if (_idx == null)
			_idx = new IndexingStatement(this);

		return _idx;
	}

	public void add(final GenerateStatement generateStatement) {
		assert gen == null;
		gen = generateStatement;
	}

	public void add(final LibraryStatementPart libraryStatementPart) {
		libraryStatementPart.setInstructions(this);
		lsps.add(libraryStatementPart);
	}

	public void setFilename(final String filename) {
		this.filename = filename;
	}

	public String getFilename() {
		return filename;
	}

	@Nullable
	public String genLang() {
		final List<GenerateStatement.Directive> gens = gen.dirs.stream()
		                                                       .filter((final GenerateStatement.Directive input) -> input.getName().equals("gen"))
		                                                       .collect(Collectors.toList());
		if (gens.size() == 0) return null;
		final IExpression lang_raw = gens.get(0).getExpression();
		assert lang_raw instanceof StringExpression;
		final String text = ((StringExpression) lang_raw).getText();
		if (text.charAt(0) == '\"') // TODO ugly
			return Helpers.remove_single_quotes_from_string(text);
		else
			return text;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setName(final Token name) {
		this.name = name.getText();
	}
}

//
//
//
