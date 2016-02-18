# Changelog

Only noting significant user changes, not internal code cleanups and minor bug fixes.

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
