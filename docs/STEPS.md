# Implemented Steps

## File System
* `findFiles` - Find/list files in the workspace. Returns an array of [FileWrapper](../src/main/java/org/jenkinsci/plugins/pipeline/utility/steps/fs/FileWrapper)s ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/fs/FindFilesStep/help.html))
* `touch` - Create a file (if not already exist) in the workspace, and set the timestamp. Returns a [FileWrapper](../src/main/java/org/jenkinsci/plugins/pipeline/utility/steps/fs/FileWrapper) representing the file that was touched. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/fs/TouchStep/help.html))

### Zip Files
* `zip` - Create Zip file. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/zip/ZipStep/help.html))
* `unzip` - Extract/Read Zip file ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/zip/UnZipStep/help.html))

### Configuration Files
* `readProperties` - Read [java properties](https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html) from files in the workspace or text. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/conf/ReadPropertiesStep/help.html))
* `readManifest` - Read a [Jar Manifest](https://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html#JAR_Manifest). ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/conf/mf/ReadManifestStep/help.html))