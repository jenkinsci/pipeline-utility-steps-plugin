package org.jenkinsci.plugins.pipeline.utility.steps.fs.FileVerifySha256Step

def f = namespace(lib.FormTagLib) as lib.FormTagLib

f.entry(field: 'file', title: _('File')) {
    f.textbox()
}

f.entry(field: 'hash', title: _('Hash')) {
    f.textbox()
}
