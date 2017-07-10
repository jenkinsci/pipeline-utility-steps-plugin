# Implemented Steps

## File System
* `findFiles` - Find/list files in the workspace. Returns an array of [FileWrapper](../src/main/java/org/jenkinsci/plugins/pipeline/utility/steps/fs/FileWrapper.java)s ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/fs/FindFilesStep/help.html))
* `touch` - Create a file (if not already exist) in the workspace, and set the timestamp. Returns a [FileWrapper](../src/main/java/org/jenkinsci/plugins/pipeline/utility/steps/fs/FileWrapper.java) representing the file that was touched. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/fs/TouchStep/help.html))

### Zip Files
* `zip` - Create Zip file. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/zip/ZipStep/help.html))
* `unzip` - Extract/Read Zip file ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/zip/UnZipStep/help.html))

### Configuration Files
* `readProperties` - Read [java properties](https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html) from files in the workspace or text. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/conf/ReadPropertiesStep/help.html))
* `propertiesWrapper` - Read [java properties](https://docs.oracle.com/javase/7/docs/api/java/util/Properties.html) from files in the workspace
  or text into the environment for a block. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/conf/PropertiesWrapperStep/help.html))
* `readManifest` - Read a [Jar Manifest](https://docs.oracle.com/javase/7/docs/technotes/guides/jar/jar.html#JAR_Manifest). ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/conf/mf/ReadManifestStep/help.html))
* `readYaml` - Read [YAML](http://yaml.org) from files in the workspace or text. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/conf/ReadYamlStep/help.html))
* `readJSON` - Read [JSON](http://www.json.org/json-it.html) from files in the workspace or text. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/json/ReadJSONStep/help.html))
* `writeJSON` - Write a [JSON](http://www.json.org/json-it.html) object to a files in the workspace. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/json/WriteJSONStep/help.html))

#### Maven Projects
* `readMavenPom` - Read a [Maven Project](https://maven.apache.org/pom.html) into a [Model](http://maven.apache.org/components/ref/3.3.9/maven-model/apidocs/org/apache/maven/model/Model.html) data structure. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/maven/ReadMavenPomStep/help.html))
* `writeMavenPom` - Write a [Model](http://maven.apache.org/components/ref/3.3.9/maven-model/apidocs/org/apache/maven/model/Model.html) data structure to a file. ([help](../src/main/resources/org/jenkinsci/plugins/pipeline/utility/steps/maven/WriteMavenPomStep/help.html))
