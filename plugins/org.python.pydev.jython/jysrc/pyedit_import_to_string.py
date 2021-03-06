from __future__ import nested_scopes # for Jython 2.1 compatibility

# Do the right thing with boolean values for all known Python versions (so this
# module can be copied to projects that don't depend on Python 2.3, e.g. Optik
# and Docutils).
try:
    True, False #@UndefinedVariable
except NameError:
    (True, False) = (1, 0)

#===============================================================================
# Pydev Extensions in Jython code protocol
#=============================================================================== 
True, False = 1, 0
if False:
    from org.python.pydev.editor import PyEdit #@UnresolvedImport
    cmd = 'command string'
    editor = PyEdit

#---------------------------- REQUIRED LOCALS-----------------------------------
# interface: String indicating which command will be executed As this script
# will be watching the PyEdit (that is the actual editor in Pydev), and this
# script will be listening to it, this string can indicate any of the methods of
# org.python.pydev.editor.IPyEditListener
assert cmd is not None 

# interface: PyEdit object: this is the actual editor that we will act upon
assert editor is not None
     
if cmd == 'onCreateActions':
    import re
    from org.eclipse.jface.action import Action #@UnresolvedImport
    from org.python.pydev.core.docutils import PySelection #@UnresolvedImport
    from org.eclipse.ui.texteditor import IEditorStatusLine #@UnresolvedImport
    from org.eclipse.swt.widgets import Display #@UnresolvedImport
    from java.lang import Runnable #@UnresolvedImport
    from org.python.pydev.plugin import PydevPlugin #@UnresolvedImport
    from org.eclipse.ui.texteditor import AbstractDecoratedTextEditorPreferenceConstants #@UnresolvedImport
    
    class ImportToString(Action):
        ''' Make a string joining the various parts available in the selection (and removing strings 'from' and 'import')        
        '''
        def run(self):
            sel = PySelection(editor)
            txt = sel.getSelectedText()
            
            splitted = re.split('\\.|\\ ', txt)
            new_text = '.'.join([x for x in splitted if x not in ('from', 'import')])
            new_text = splitted[-1] + ' = ' + '\'' + new_text + '\'' 
            doc = sel.getDoc()
            sel = sel.getTextSelection()
            doc.replace(sel.getOffset(), sel.getLength(), new_text)
 


    # Change these constants if the default does not suit your needs
    ACTIVATION_STRING = 'is'
    WAIT_FOR_ENTER = False
    
    # Register the extension as an ActionListener.    
    editor.addOfflineActionListener(ACTIVATION_STRING, ImportToString(), \
                                    'Import to string', \
                                    WAIT_FOR_ENTER)
