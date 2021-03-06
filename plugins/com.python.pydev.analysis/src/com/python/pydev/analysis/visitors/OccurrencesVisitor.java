/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 19/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.callbacks.ICallbackListener;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStack;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.decoratorsType;

import com.aptana.shared_core.structure.Tuple;
import com.python.pydev.analysis.IAnalysisPreferences;
import com.python.pydev.analysis.messages.IMessage;
import com.python.pydev.analysis.scopeanalysis.AbstractScopeAnalyzerVisitor;

/**
 * This visitor marks the used/ unused tokens and generates the messages related
 * 
 * @author Fabio
 */
public final class OccurrencesVisitor extends AbstractScopeAnalyzerVisitor {

    /**
     * Used to manage the messages
     */
    public final MessagesManager messagesManager;

    /**
     * Used to check for duplication in signatures
     */
    private final DuplicationChecker duplicationChecker;

    /**
     * Used to check if a signature from a method starts with self (if it is not a staticmethod)
     */
    private final NoSelfChecker noSelfChecker;

    /**
     * Used to check arguments.
     */
    private final ArgumentsChecker argumentsChecker;

    /**
     * Determines whether we should check if function call arguments actually match the signature of the object being 
     * called.
     */
    private final boolean analyzeArgumentsMismatch;

    public OccurrencesVisitor(IPythonNature nature, String moduleName, IModule current, IAnalysisPreferences prefs,
            IDocument document, IProgressMonitor monitor) {
        super(nature, moduleName, current, document, monitor);
        this.messagesManager = new MessagesManager(prefs, moduleName, document);

        this.analyzeArgumentsMismatch = prefs.getSeverityForType(IAnalysisPreferences.TYPE_ARGUMENTS_MISATCH) > IMarker.SEVERITY_INFO; //Don't even run checks if we don't raise at least a warning.
        if (this.analyzeArgumentsMismatch) {
            this.argumentsChecker = new ArgumentsChecker(this);
        } else {
            //Don't even create it if we're not going to use it.
            this.argumentsChecker = null;
        }

        this.duplicationChecker = new DuplicationChecker(this);
        this.noSelfChecker = new NoSelfChecker(this);
    }

    private int isInTestScope = 0;

    @Override
    public Object visitCompare(Compare node) throws Exception {
        Object ret = super.visitCompare(node);
        if (isInTestScope == 0) {
            SourceToken token = AbstractVisitor.makeToken(node, moduleName);
            messagesManager.addMessage(IAnalysisPreferences.TYPE_NO_EFFECT_STMT, token);
        }
        return ret;
    }

    public void traverse(If node) throws Exception {
        checkStop();
        if (node.test != null) {
            isInTestScope += 1;
            node.test.accept(this);
            isInTestScope -= 1;
        }

        if (node.body != null) {
            for (int i = 0; i < node.body.length; i++) {
                if (node.body[i] != null)
                    node.body[i].accept(this);
            }
        }
        if (node.orelse != null) {
            node.orelse.accept(this);
        }
    }

    @Override
    public Object visitTuple(org.python.pydev.parser.jython.ast.Tuple node) throws Exception {
        isInTestScope += 1;
        Object ret = super.visitTuple(node);
        isInTestScope -= 1;
        return ret;
    }

    public void traverse(While node) throws Exception {
        checkStop();
        if (node.test != null) {
            isInTestScope += 1;
            node.test.accept(this);
            isInTestScope -= 1;
        }

        if (node.body != null) {
            for (int i = 0; i < node.body.length; i++) {
                if (node.body[i] != null)
                    node.body[i].accept(this);
            }
        }
        if (node.orelse != null)
            node.orelse.accept(this);
    }

    @Override
    public Object visitRaise(Raise node) throws Exception {
        isInTestScope += 1;
        Object r = super.visitRaise(node);
        isInTestScope -= 1;
        return r;
    }

    @Override
    public Object visitComprehension(Comprehension node) throws Exception {
        isInTestScope += 1;
        Object r = super.visitComprehension(node);
        isInTestScope -= 1;
        return r;

    }

    @Override
    public Object visitAssert(Assert node) throws Exception {
        isInTestScope += 1;
        Object r = super.visitAssert(node);
        isInTestScope -= 1;
        return r;
    }

    @Override
    public Object visitPrint(Print node) throws Exception {
        isInTestScope += 1;
        Object r = super.visitPrint(node);
        isInTestScope -= 1;
        return r;
    }

    @Override
    public Object visitAssign(Assign node) throws Exception {
        isInTestScope += 1;
        Object r = super.visitAssign(node);
        isInTestScope -= 1;

        if (analyzeArgumentsMismatch) {
            argumentsChecker.visitAssign(node);
        }
        return r;
    }

    @Override
    public Object visitYield(Yield node) throws Exception {
        isInTestScope += 1;
        Object r = super.visitYield(node);
        isInTestScope -= 1;
        return r;
    }

    @Override
    public Object visitAugAssign(AugAssign node) throws Exception {
        isInTestScope += 1;
        Object r = super.visitAugAssign(node);
        isInTestScope -= 1;
        return r;
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        isInTestScope += 1;
        Object r = super.visitCall(node);
        isInTestScope -= 1;
        return r;
    }

    @Override
    public Object visitReturn(Return node) throws Exception {
        isInTestScope += 1;
        Object r = super.visitReturn(node);
        isInTestScope -= 1;
        return r;
    }

    @Override
    protected void handleDecorator(decoratorsType dec) throws Exception {
        isInTestScope += 1;
        dec.accept(this);
        isInTestScope -= 1;
    }

    @Override
    public Object visitLambda(Lambda node) throws Exception {
        isInTestScope += 1;
        Object ret = super.visitLambda(node);
        isInTestScope -= 1;
        return ret;
    }

    public void traverse(SimpleNode node) throws Exception {
        if (node instanceof If) {
            traverse((If) node);
        } else if (node instanceof While) {
            traverse((While) node);
        } else if (node instanceof ListComp) {
            this.visitListComp((ListComp) node);
        } else {
            super.traverse(node);
        }
    }

    /**
     * @return the generated messages.
     */
    public List<IMessage> getMessages() {
        endScope(null); //have to end the scope that started when we created the class.

        return messagesManager.getMessages();
    }

    /**
     * @param foundTok
     */
    protected void onAddUndefinedVarInImportMessage(IToken foundTok, Found foundAs) {
        messagesManager.addUndefinedVarInImportMessage(foundTok, foundTok.getRepresentation());
    }

    /**
     * @param foundTok
     */
    protected void onAddAssignmentToBuiltinMessage(IToken foundTok, String representation) {
        messagesManager.onAddAssignmentToBuiltinMessage(foundTok, representation);
    }

    /**
     * @param token
     */
    protected void onAddUndefinedMessage(IToken token, Found foundAs) {
        if ("...".equals(token.getRepresentation())) {
            return; //Ellipsis -- when found in the grammar, it's added as a name, which we can safely ignore at this point.
        }

        //global scope, so, even if it is defined later, this is an error...
        messagesManager.addUndefinedMessage(token);
    }

    /**
     * @param m
     */
    protected void onLastScope(ScopeItems m) {
        for (Found n : probablyNotDefined) {
            String rep = n.getSingle().tok.getRepresentation();
            Map<String, Tuple<IToken, Found>> lastInStack = m.namesToIgnore;
            if (scope.findInNamesToIgnore(rep, lastInStack) == null) {
                onAddUndefinedMessage(n.getSingle().tok, n);
            }
        }
    }

    /**
     * @param reportUnused
     * @param m
     */
    protected void onAfterEndScope(SimpleNode node, ScopeItems m) {
        boolean reportUnused = true;
        if (node != null && node instanceof FunctionDef) {
            reportUnused = !isVirtual((FunctionDef) node);
        }

        if (reportUnused) {
            //so, now, we clear the unused
            int scopeType = m.getScopeType();
            for (List<Found> list : m.getAll()) {
                int len = list.size();
                for (int i = 0; i < len; i++) {
                    Found f = list.get(i);
                    if (!f.isUsed()) {
                        // we don't get unused at the global scope or class definition scope unless it's an import
                        if ((scopeType & Scope.ACCEPTED_METHOD_AND_LAMBDA) != 0 || f.isImport()) { //only within methods do we put things as unused 
                            messagesManager.addUnusedMessage(node, f);
                        }
                    }
                }
            }
        }
    }

    /**
     * A method is virtual if it contains only raise and string statements 
     */
    protected boolean isVirtual(FunctionDef node) {
        if (node.body != null) {
            int len = node.body.length;
            for (int i = 0; i < len; i++) {
                SimpleNode n = node.body[i];
                if (n instanceof Raise) {
                    continue;
                }
                if (n instanceof Expr) {
                    if (((Expr) n).value instanceof Str) {
                        continue;
                    }
                }
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onAfterStartScope(int newScopeType, SimpleNode node) {
        if (newScopeType == Scope.SCOPE_TYPE_CLASS) {
            duplicationChecker.beforeClassDef((ClassDef) node);
            noSelfChecker.beforeClassDef((ClassDef) node);

        } else if ((newScopeType & Scope.SCOPE_TYPE_METHOD) != 0) {
            duplicationChecker.beforeFunctionDef((FunctionDef) node); //duplication checker
            noSelfChecker.beforeFunctionDef((FunctionDef) node);
        }
    }

    @Override
    protected void onBeforeEndScope(SimpleNode node) {
        if (node instanceof ClassDef) {
            noSelfChecker.afterClassDef((ClassDef) node);
            duplicationChecker.afterClassDef((ClassDef) node);

        } else if (node instanceof FunctionDef) {
            duplicationChecker.afterFunctionDef((FunctionDef) node);//duplication checker
            noSelfChecker.afterFunctionDef((FunctionDef) node);
        }
    }

    @Override
    public void onAddUnusedMessage(SimpleNode node, Found found) {
        messagesManager.addUnusedMessage(node, found);
    }

    @Override
    public void onAddReimportMessage(Found newFound) {
        messagesManager.addReimportMessage(newFound);
    }

    @Override
    public void onAddUnresolvedImport(IToken token) {
        messagesManager.addMessage(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, token);
    }

    @Override
    protected void onAfterVisitAssign(Assign node) {
        noSelfChecker.visitAssign(node);
    }

    @Override
    protected void onVisitCallFunc(final Call callNode) throws Exception {
        if (!analyzeArgumentsMismatch) {
            super.onVisitCallFunc(callNode);
        } else {
            if (callNode.func instanceof Name) {
                Name name = (Name) callNode.func;
                startRecordFound();
                visitName(name);

                //Check if the name was actually found in some way...
                TokenFoundStructure found = popFound();
                if (found != null && found.token instanceof SourceToken) {
                    final SourceToken sourceToken = (SourceToken) found.token;
                    if (found.defined) {
                        argumentsChecker.checkNameFound(callNode, sourceToken);
                    } else if (found.found != null) {
                        //Still not found: register a callback to be called if it's found later on.
                        found.found.registerCallOnDefined(new ICallbackListener<Found>() {

                            public Object call(Found f) {
                                try {
                                    List<GenAndTok> all = f.getAll();
                                    for (GenAndTok genAndTok : all) {
                                        if (genAndTok.tok instanceof SourceToken) {
                                            SourceToken sourceToken2 = (SourceToken) genAndTok.tok;
                                            if (sourceToken2.getAst() instanceof FunctionDef
                                                    || sourceToken2.getAst() instanceof ClassDef) {
                                                argumentsChecker.checkNameFound(callNode, sourceToken2);
                                                return null;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    Log.log(e);
                                }
                                return null;
                            }
                        });
                    }
                }

            } else {
                startRecordFound();
                callNode.func.accept(this);
                TokenFoundStructure found = popFound();
                argumentsChecker.checkAttrFound(callNode, found);
            }
        }

    }

    public static final class TokenFoundStructure {

        public final IToken token;
        public final boolean defined;
        public final Found found;

        /**
         * @param foundForProbablyNotDefined if not defined, the token used is passed on so that if it gets later defined,
         * a notification may be gotten.
         */
        public TokenFoundStructure(IToken token, boolean defined, Found found) {
            this.token = token;
            this.defined = defined;
            this.found = found;
        }

    }

    private final FastStack<TokenFoundStructure> recordedFounds = new FastStack<TokenFoundStructure>(4);
    private int recordFounds = 0;

    private void onPushToRecordedFounds(IToken o1) {
        if (recordFounds > 0) {
            recordedFounds.push(new TokenFoundStructure(o1, true, null));
        }
    }

    /**
     * Called when a token is not found.
     */
    @Override
    protected void onAddToProbablyNotDefined(IToken token, Found foundForProbablyNotDefined) {
        if (recordFounds > 0) {
            recordedFounds.push(new TokenFoundStructure(token, false, foundForProbablyNotDefined));
        }
    }

    /**
     * Gets the token which was found and whether it was actually defined at that time (otherwise, it may be that
     * it'll only be defined later on, in which case the check will have to be done later on too -- and only if it
     * was really defined).
     */
    protected TokenFoundStructure popFound() {
        recordFounds -= 1;
        if (recordedFounds.size() > 0) {
            TokenFoundStructure ret = recordedFounds.peek();
            recordedFounds.clear();
            return ret;
        }
        return null;
    }

    protected void startRecordFound() {
        recordFounds += 1;
    }

    @Override
    protected void onFoundTokenAs(IToken token, Found foundAs) {
        if (analyzeArgumentsMismatch) {
            boolean reportFound = true;
            try {
                if (foundAs.importInfo != null) {
                    IDefinition[] definition = foundAs.importInfo.getDefinitions(nature, completionCache);
                    for (IDefinition iDefinition : definition) {
                        Definition d = (Definition) iDefinition;
                        if (d.ast instanceof FunctionDef || d.ast instanceof ClassDef) {
                            SourceToken tok = AbstractVisitor.makeToken(d.ast, token.getRepresentation(),
                                    d.module != null ? d.module.getName() : "");
                            tok.setDefinition(d);
                            onPushToRecordedFounds(tok);
                            reportFound = false;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
            if (reportFound) {
                onPushToRecordedFounds(token);
            }
        }
    }

    @Override
    protected void onFoundInNamesToIgnore(IToken token, IToken tokenInNamesToIgnore) {
        if (analyzeArgumentsMismatch) {
            if (tokenInNamesToIgnore instanceof SourceToken) {
                SourceToken sourceToken = (SourceToken) tokenInNamesToIgnore;
                //Make a new token because we want the ast to be the FunctionDef or ClassDef, not the name which is the reference.
                onPushToRecordedFounds(AbstractVisitor.makeToken(sourceToken.getAst(), token.getRepresentation(),
                        sourceToken.getParentPackage()));
            }
        }
    }

}
