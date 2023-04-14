package org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep
/*
  ~ The MIT License (MIT)
  ~
  ~ Copyright (c) 2016 CloudBees Inc.
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining a copy
  ~ of this software and associated documentation files (the "Software"), to deal
  ~ in the Software without restriction, including without limitation the rights
  ~ to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
  ~ copies of the Software, and to permit persons to whom the Software is
  ~ furnished to do so, subject to the following conditions:
  ~
  ~ The above copyright notice and this permission notice shall be included in all
  ~ copies or substantial portions of the Software.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
  ~ IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  ~ FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
  ~ AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
  ~ LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  ~ OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
  ~ SOFTWARE.
 */
p {
    text('Reads a file in the current working directory or a String as a plain text ')
    a(href: 'http://yaml.org', target: '_blank') {
        text('YAML')
    }
    text(' file. ')
    text('It uses ')
    a(href: 'https://bitbucket.org/asomov/snakeyaml', target: '_blank') {
        text('SnakeYAML')
    }
    text(' as YAML processor. ')
    text('The returned objects are standard Java objects like List, Long, String, ...: ')
    ul {
		li('bool: [true, false, on, off]')
        li('int: 42')
        li('float: 3.14159')
        li('list: [\'LITE\', \'RES_ACID\', \'SUS_DEXT\']')
        li('map: {hp: 13, sp: 5}')
    }
}
strong('Fields: ')
ul {
    li {
        code('file: ')
        text('Optional path to a file in the workspace to read the YAML data from.')
    }
    li {
        code('text: ')
        text('An Optional String containing YAML formatted data. ')
        em {
            text('These are added to the resulting object after ')
            code('file')
            text(' and so will overwrite any value already present if not a new YAML document.')
        }
    }
    li {
        code('codePointLimit: ')
        text('Limit for incoming data in bytes. ')
        text('Defaults to 3145728 (3MB) if not set.')
        br()
        em {
            String max_code_point_limit_property = org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.MAX_CODE_POINT_LIMIT_PROPERTY
            String default_code_point_limit_property = org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.DEFAULT_CODE_POINT_LIMIT_PROPERTY
            int max_code_point_limit = org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.getMaxCodePointLimit()
            int default_code_point_limit = org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.getDefaultCodePointLimit()
            text("""\
                There is a maximum value you can set on this controller: ${max_code_point_limit}.
                The administrator can change the max allowed value by setting the System property: ${max_code_point_limit_property}.
                The default can also be changed by setting the System property: ${default_code_point_limit_property}
                so that no pipeline code needs to be changed. """.stripIndent())
            if (default_code_point_limit >= -1) {
                br()
                text("On this controller the default is set to ${default_code_point_limit}.")
            }
        }
    }
    li {
        code('maxAliasesForCollections: ')
        text('Restrict the amount of aliases for collections (sequences and mappings) to avoid ')
        a(href:'https://en.wikipedia.org/wiki/Billion_laughs_attack') {
            text('Billion laughs attack. ')
        }
        text('The default is set by the YAML processor to 50, and you can override it with this parameter.')
        br()
        strong('WARNING: ')
        text('''\
            Actions performed using this step are executed on the controller. Increasing the value here increases the risk of the controller being overburdened.
            Consider using an alternative approach such as a having a standalone script doing the work (e.g. using yq to filter the giant document instead).
            '''.stripIndent())
        br()
        text('Excessive use is likely an antipattern per ')
        a(href:'https://www.jenkins.io/doc/book/pipeline/pipeline-best-practices/') {
            text('Pipeline Best Practices.')
        }
        br()
        em {
            String max_max_aliases_property = org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.MAX_MAX_ALIASES_PROPERTY
            String default_max_aliases_property = org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.DEFAULT_MAX_ALIASES_PROPERTY
            int max_max_aliases_for_collections = org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.getMaxMaxAliasesForCollections()
            int default_max_aliases_for_collections = org.jenkinsci.plugins.pipeline.utility.steps.conf.ReadYamlStep.getDefaultMaxAliasesForCollections()
            text("""\
                There is a maximum value you can set on this controller: ${max_max_aliases_for_collections}.
                The administrator can change the max allowed value by setting the System property: ${max_max_aliases_property}.
                The default can also be changed by setting the System property: ${default_max_aliases_property}
                so that no pipeline code needs to be changed. """.stripIndent())
            if (default_max_aliases_for_collections >= 0) {
                br()
                text("On this controller the default is set to ${default_max_aliases_for_collections}.")
            }
        }
    }
}
p {
    strong('Examples : ')
    br()
    text('With only one YAML document :')
    code {
        pre('''\
            def datas = readYaml text: """
            something: 'my datas'
            size: 3
            isEmpty: false
            """
            assert datas.something == 'my datas'
            assert datas.size == 3
            assert datas.isEmpty == false
            '''.stripIndent())
    }
    text('With several YAML documents:')
    code {
        pre('''\
            def datas = readYaml text: """
            ---
            something: 'my first document'
            ---
            something: 'my second document'
            """
            assert datas.size() == 2
            assert datas[0].something == 'my first document'
            assert datas[1].something == 'my second document'
            '''.stripIndent())
    }
    text('With file dir / my.yml containing')
    code('something : \'my datas\'')
    text(':')
    code {
        pre('''\
            def datas = readYaml file: 'dir/ my.yml ', text: "something: ' Override '"
            assert datas.something == 'Override'
            '''.stripIndent())
    }
}