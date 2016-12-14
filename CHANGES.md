# Changelog

## 1.2.2 (Dec 14, 2016)
* [JENKINS-34122](https://issues.jenkins-ci.org/browse/JENKINS-34122) zip: Exclude output file from itself. _([PR #21](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/21), [PR #19](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/19), [PR #18](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/18))_

## 1.2.1 (Nov 30, 2016)
* Added the ability to specify charset on unzip _([PR #20](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/20))_

## 1.2.0 (Nov 21, 2016)
* Added step: `readYaml` _([PR #17](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/17))_

## 1.1.6 (Jun 21, 2016)

* [JENKINS-34927](https://issues.jenkins-ci.org/browse/JENKINS-34927) - Close streams of extracted files _([PR #14](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/14))_
* [JENKINS-34935](https://issues.jenkins-ci.org/browse/JENKINS-34935) - Fixed detection of corrupt zip files _([PR #15](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/15))_

## 1.1.5 (Apr 18, 2016)
* Corrected the help text for the `dir` property on the `unzip` step. [pr #13](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/13)

## 1.1.4 (Feb 29, 2016)
* Fixed another potential `NullPointerException` in ReadMavenPomStep whitelist. [pr #12](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/12)

## 1.1.3 (Feb 18, 2016)
* Changed unzip file reading to whole file instead of line by line to fix another issue in `readManifest`. [pr #11](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/11)

## 1.1.2 (Feb 17, 2016)
* Fixed file encoding in `readManifest`. [pr #10](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/10)

## 1.1.1 (Feb 17, 2016)
* Fixed null protection to ReadMavenPomStep whitelist. [pr #9](https://github.com/jenkinsci/pipeline-utility-steps-plugin/pull/9)

## 1.1 (Feb 3, 2016)
* Added steps: `readMavenPom`, `writeMavenPom`.

## 1.0 (Jan 18, 2016)
* First release
