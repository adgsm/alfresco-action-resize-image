# Alfresco Folder Action AMP, image multiplication and optimization for web use per upload to a folder

Multiplication of uploaded image in web standard sizes/resolutions

### Usage

#### Create AMP
```
mvn clean install
```
#### Install AMP
```
/opt/alfresco/bin/apply_amps.sh
```
or
```
java -jar /opt/alfresco/bin/alfresco-mmt.jar install alfresco-action-resize-image /opt/alfresco/tomcat/webapps/alfresco.war
```

### License
Licensed under the MIT license.
http://www.opensource.org/licenses/mit-license.php
