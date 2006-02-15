/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.IOException;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.Attribute;
import org.python.parser.ast.BinOp;
import org.python.parser.ast.Call;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Compare;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.If;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.Num;
import org.python.parser.ast.Pass;
import org.python.parser.ast.Print;
import org.python.parser.ast.Str;
import org.python.parser.ast.Subscript;
import org.python.parser.ast.Tuple;
import org.python.parser.ast.UnaryOp;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.Yield;
import org.python.parser.ast.argumentsType;
import org.python.parser.ast.decoratorsType;
import org.python.parser.ast.exprType;
import org.python.parser.ast.keywordType;

/**
 * statements that 'need' to be on a new line:
 * print
 * del
 * pass
 * flow
 * import
 * global
 * exec
 * assert
 * 
 * 
 * flow:
 * return
 * yield
 * raise
 * 
 * 
 * compound:
 * if
 * while
 * for
 * try
 * func
 * class
 * 
 * @author Fabio
 */
public class PrettyPrinter extends VisitorBase{

    protected PrettyPrinterPrefs prefs;
    private WriteState state;
    private AuxSpecials auxComment;

    public PrettyPrinter(PrettyPrinterPrefs prefs, IWriterEraser writer){
        this.prefs = prefs;
        state = new WriteState(writer, prefs);
        auxComment = new AuxSpecials(state, writer, prefs);
    }
    
    @Override
    public Object visitAssign(Assign node) throws Exception {
    	state.pushInStmt(node);
        auxComment.writeSpecialsBefore(node);
        for (SimpleNode target : node.targets) {
            target.accept(this);
        }
        state.write(" = ");
        auxComment.startRecord();
        node.value.accept(this);
        auxComment.writeSpecialsAfter(node);
        
        checkEndRecord();
        state.popInStmt();
        return null;
    }

    private void checkEndRecord() throws IOException {
        checkEndRecord(null);
        
    }
    /**
     * @param node 
     * @throws IOException
     */
    private void checkEndRecord(SimpleNode node) throws IOException {
        if(node != null){
            auxComment.writeSpecialsAfter(node);
        }

        if(!auxComment.endRecord().writtenComment){
            state.writeNewLine();
            state.writeIndent();
        }
    }
    
    @Override
    public Object visitUnaryOp(UnaryOp node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.write(node.operand.toString());
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    
    public static final String[] operatorMapping = new String[] {
        "<undef>",
        " + ",
        " - ",
        " * ",
        " / ",
        " % ",
        " ** ",
        " << ",
        " >> ",
        " | ",
        " ^ ",
        " & ",
        " // ",
    };

    @Override
    public Object visitBinOp(BinOp node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.pushInStmt(node);
        node.left.accept(this);
        state.write(operatorMapping[node.op]);
        node.right.accept(this);
        state.popInStmt();

        auxComment.startRecord();
        auxComment.writeSpecialsAfter(node);
        
        if(!state.inStmt()){
            checkEndRecord();
        }
        return null;
    }
    
    public static final String[] cmpop= new String[] {
        "<undef>",
        " == ",
        " != ",
        "Lt",
        "LtE",
        "Gt",
        "GtE",
        " is ",
        " is not ",
        " in ",
        " not in ",
    };

    @Override
    public Object visitCompare(Compare node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        node.left.accept(this);
        
        for(int op : node.ops){
            state.write(cmpop[op]);
        }
        
        for (SimpleNode n : node.comparators){
            n.accept(this);
        }
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    @Override
    public Object visitTuple(Tuple node) throws Exception {
    	auxComment.writeSpecialsBefore(node);
    	super.visitTuple(node);
    	auxComment.writeSpecialsAfter(node);
    	return null;
    }
    
    @Override
    public Object visitNum(Num node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.write(node.n.toString());
        auxComment.writeSpecialsAfter(node);
        return null;
    }
    
    @Override
    public Object visitSubscript(Subscript node) throws Exception {
    	auxComment.writeSpecialsBefore(node);
    	super.visitSubscript(node);
    	auxComment.writeSpecialsAfter(node);
    	return null;
    }
    
    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        node.value.accept(this);
        state.write(".");
        node.attr.accept(this);
        return null;
    }
    
    @Override
    public Object visitPrint(Print node) throws Exception {
    	auxComment.writeSpecialsBefore(node);
    	state.pushInStmt(node);
    	auxComment.startRecord();
    	super.visitPrint(node);
    	state.popInStmt();
    	checkEndRecord(node);
    	return null;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        
        //make the visit
        state.pushInStmt(node);
        node.func.accept(this);
        state.popInStmt();
        auxComment.writeSpecialsBefore(node);
        exprType[] args = node.args;
        state.indent();
        for (int i = 0; i < args.length; i++) {
            if (args[i] != null){
                boolean last = i == args.length-1;
                if(last){
                    auxComment.startRecord();
                }
                args[i].accept(this);
            }
        }
        dedent();
        if(args.length == 0){
            auxComment.startRecord();
        }
        keywordType[] keywords = node.keywords;
        if (keywords != null) {
            for (int i = 0; i < keywords.length; i++) {
                if (keywords[i] != null){
                    auxComment.writeSpecialsBefore(keywords[i]);
                    state.indent();
                    keywords[i].accept(this);
                    dedent();
                    auxComment.writeSpecialsAfter(keywords[i]);
                }
            }
        }
        exprType starargs = node.starargs;
        if (starargs != null){
            starargs.accept(this);
        }
        exprType kwargs = node.kwargs;
        if (kwargs != null){
            kwargs.accept(this);
        }
        
        auxComment.writeCommentsAfter(node);
        if(auxComment.endRecord().writtenComment){
            //some comment was written
            state.writeIndent(1);
        }
        auxComment.writeStringsAfter(node);
        if(!state.inStmt()){
            state.writeNewLine();
        }
        return null;
    }
    
    @Override
    public Object visitIf(If node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        auxComment.startRecord();
        node.test.accept(this);
        
        //write the 'if test:'
        makeIfIndent();
		
		//write the body and dedent
        for (SimpleNode n : node.body){
            auxComment.writeSpecialsBefore(n);
            n.accept(this);
            auxComment.writeSpecialsAfter(n);
        }
        dedent();
        
        
        if(node.orelse != null && node.orelse.length > 0){
        	boolean inElse = false;
        	auxComment.startRecord();
            auxComment.writeSpecialsAfter(node);
            
            //now, if it is an elif, it will end up calling the 'visitIf' again,
            //but if it is an 'else:' we will have to make the indent again
            if(node.specialsAfter.contains("else:")){
            	inElse = true;
            	makeIfIndent();
            }
            for (SimpleNode n : node.orelse) {
                auxComment.writeSpecialsBefore(n);
                n.accept(this);
//                auxComment.writeSpecialsAfter(n); // same as the initial
            }
            if(inElse){
            	dedent();
            }
        }
        
        return null;
    }

	private void makeIfIndent() throws IOException {
		state.indent();
        boolean writtenComment = auxComment.endRecord().writtenComment;
		if(!writtenComment){
        	state.writeNewLine();
        }
		state.writeIndent();
	}
    
    private static final String[] strTypes = new String[]{
        "'''",
        "\"\"\"",
        "'",
        "\""
    };
    
    @Override
    public Object visitStr(Str node) throws Exception {
    	auxComment.writeSpecialsBefore(node);
        if(node.unicode){
            state.write("u");
        }
        if(node.raw){
            state.write("r");
        }
        final String s = strTypes[node.type-1];
        
    	state.write(s);
    	state.write(node.s);
    	state.write(s);
    	if(!state.inStmt()){
	    	state.writeNewLine();
	    	state.writeIndent();
    	}
    	auxComment.writeSpecialsAfter(node);
    	return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        fixNewStatementCondition();

        auxComment.writeSpecialsBefore(node.name);
        auxComment.writeSpecialsBefore(node);
        state.write("class ");
        
        
        NameTok name = (NameTok) node.name;

        auxComment.startRecord();
        state.write(name.id);
        //we want the comments to be indented too
        state.indent();
        {
        	auxComment.writeSpecialsAfter(name);
    
            if(node.bases.length > 0){
//                state.write("(");
                for (exprType expr : node.bases) {
                    expr.accept(this);
                }
            }
            checkEndRecord();
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            dedent();
        }   
        auxComment.writeSpecialsAfter(node);
        return null;
    }

    /**
     * 
     */
    private void dedent() {
        if(state.lastIsIndent()){
            state.eraseIndent();
        }
        state.dedent();
    }

    @Override
    public Object visitYield(Yield node) throws Exception {
        fixNewStatementCondition();
        state.write("yield ");
        beforeNode(node);
        super.visitYield(node);
        afterNode(node);
        return null;
    }

    private void beforeNode(SimpleNode node) throws IOException {
        auxComment.writeSpecialsBefore(node);
        auxComment.startRecord();
    }

    private void afterNode(SimpleNode node) throws IOException {
        checkEndRecord(node);
    }
    
    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        fixNewStatementCondition();
        decoratorsType[] decs = node.decs;
        for (decoratorsType dec : decs) {
            auxComment.writeSpecialsBefore(dec);
            auxComment.writeSpecialsAfter(dec);
        }
        auxComment.writeSpecialsBefore(node);
        auxComment.writeSpecialsBefore(node.name);
        state.write("def ");
        
        
        NameTok name = (NameTok) node.name;
        state.write(name.id);
        auxComment.writeSpecialsAfter(node);
        auxComment.startRecord();
        auxComment.writeSpecialsAfter(node.name);
        boolean writtenNewLine = auxComment.endRecord().writtenComment;
        state.indent();
        if(writtenNewLine){
        	state.writeIndent();
        }
        
        {
        	//arguments
        	writtenNewLine = makeArgs(node.args.args, node.args) || writtenNewLine;
        	//end arguments
        	if(!writtenNewLine){
        		state.writeNewLine();
        		state.writeIndent();
        	}
            for(SimpleNode n: node.body){
                n.accept(this);
            }
        
            dedent();
        }
        return null;
    }

    /**
     * @throws IOException
     */
    private void fixNewStatementCondition() throws IOException {
        if(state.lastIsWrite()){
            state.writeNewLine();
            state.writeIndent();
        }
    }
    
    private boolean makeArgs(exprType[] args, argumentsType completeArgs) throws Exception {
        boolean written = false;
        exprType[] d = completeArgs.defaults;
        int argsLen = args.length;
        int defaultsLen = d.length;
        int diff = argsLen-defaultsLen;
        
        int i = 0;
        for (exprType type : args) {
            auxComment.startRecord();
            if(i >= diff){
                state.pushTempBuffer();
                exprType arg = d[i-diff];
                if(arg != null){
                    arg.accept(this);
                    type.specialsAfter.add(0, state.popTempBuffer());
                    type.specialsAfter.add(0, "=");
                }else{
                    state.popTempBuffer();
                }
            }
            type.accept(this);
            written = auxComment.endRecord().writtenComment;
            i++;
        }
        return written;
    }

    @Override
    public Object visitPass(Pass node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.write("pass");
        auxComment.startRecord();
        checkEndRecord(node);
        return null;
    }
    
    @Override
    public Object visitName(Name node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.write(node.id);
        auxComment.writeStringsAfter(node);
        auxComment.writeCommentsAfter(node);
        return null;
    }
    
    @Override
    public Object visitNameTok(NameTok node) throws Exception {
        auxComment.writeSpecialsBefore(node);
        state.write(node.id);
        auxComment.writeSpecialsAfter(node);
        return null;
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

}
