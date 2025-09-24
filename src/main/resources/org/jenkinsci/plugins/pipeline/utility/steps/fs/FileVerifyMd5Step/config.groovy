package org.jenkinsci.plugins.pipeline.utility.steps.fs.FileVerifyMd5Step

import lib.FormTagLib

def f = namespace(FormTagLib) as FormTagLib

f.entry(field: 'file', title: _('File')) {
    f.textbox()
}

f.entry(field: 'hash', title: _('Hash')) {
    f.textbox()
}
