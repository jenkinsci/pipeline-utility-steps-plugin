/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 CloudBees Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package org.jenkinsci.plugins.pipeline.utility.steps.zip.UnZipStep

def f = namespace(lib.FormTagLib) as lib.FormTagLib

f.entry(field: 'zipFile', title: _('Zip File')) {
    f.textbox()
}

f.entry(field: 'dir', title: _('Directory')) {
    f.textbox()
}

f.entry(field: 'charset', title: _('Charset')) {
   f.textbox()
}

f.entry(field: 'glob', title: _('Glob')) {
    f.textbox()
}
f.entry(field: 'test', title: _('Test the archive')) {
    f.checkbox()
}
f.entry(field: 'read', title: _('Read the file contents')) {
    f.checkbox()
}


