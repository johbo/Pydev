package org.python.pydev.refactoring.ast.adapters;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.stmtType;

public class ClassDefAdapterFromClassDef implements IClassDefAdapter {

	private ClassDef classDef;

	public ClassDefAdapterFromClassDef(ClassDef classDef) {
		this.classDef = classDef;
	}

	public List<SimpleAdapter> getAssignedVariables() {
		throw new RuntimeException("Not implemented");
	}

	public List<SimpleAdapter> getAttributes() {
		throw new RuntimeException("Not implemented");
	}

	public List<String> getBaseClassNames() {
		throw new RuntimeException("Not implemented");
	}

	public List<IClassDefAdapter> getBaseClasses() {
		throw new RuntimeException("Not implemented");
	}

	public FunctionDefAdapter getFirstInit() {
		throw new RuntimeException("Not implemented");
	}

	public List<FunctionDefAdapter> getFunctions() {
		throw new RuntimeException("Not implemented");
	}

	public List<FunctionDefAdapter> getFunctionsInitFiltered() {
		ArrayList<FunctionDefAdapter> ret = new ArrayList<FunctionDefAdapter>();
		for(stmtType b:this.classDef.body){
			if(b instanceof FunctionDef){
				ret.add(new FunctionDefAdapter(null, null, (FunctionDef)b));
			}
		}
		return ret;
	}

	public int getNodeBodyIndent() {
		throw new RuntimeException("Not implemented");
	}

	public List<PropertyAdapter> getProperties() {
		throw new RuntimeException("Not implemented");
	}

	public boolean hasAttributes() {
		throw new RuntimeException("Not implemented");
	}

	public boolean hasBaseClass() {
		return false;
	}

	public boolean hasFunctions() {
		throw new RuntimeException("Not implemented");
	}

	public boolean hasFunctionsInitFiltered() {
		return true;
	}

	public boolean hasInit() {
		throw new RuntimeException("Not implemented");
	}

	public boolean isNested() {
		throw new RuntimeException("Not implemented");
	}

	public boolean isNewStyleClass() {
		throw new RuntimeException("Not implemented");
	}

	public String getName() {
		return ((NameTok)this.classDef.name).id;
	}

	public String getParentName() {
		throw new RuntimeException("Not implemented");
	}

	public ClassDef getASTNode() {
		throw new RuntimeException("Not implemented");
	}

	public SimpleNode getASTParent() {
		throw new RuntimeException("Not implemented");
	}

	public ModuleAdapter getModule() {
		throw new RuntimeException("Not implemented");
	}

	public int getNodeFirstLine() {
		throw new RuntimeException("Not implemented");
	}

	public int getNodeIndent() {
		throw new RuntimeException("Not implemented");
	}

	public int getNodeLastLine() {
		throw new RuntimeException("Not implemented");
	}

	public AbstractNodeAdapter getParent() {
		throw new RuntimeException("Not implemented");
	}

	public SimpleNode getParentNode() {
		throw new RuntimeException("Not implemented");
	}

	public boolean isModule() {
		throw new RuntimeException("Not implemented");
	}

}
