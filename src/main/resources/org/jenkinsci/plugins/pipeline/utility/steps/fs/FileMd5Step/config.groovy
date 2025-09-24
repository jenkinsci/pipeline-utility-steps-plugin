package org.jenkinsci.plugins.pipeline.utility.steps.fs.FileMd5Step

import lib.FormTagLib

def f = namespace(FormTagLib) as FormTagLib

f.entry(field: 'file', title: _('File')) {
    f.textbox()
}
