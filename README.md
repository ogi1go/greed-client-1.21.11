# GREED — My Cool Cheat

To build this mod just se
```powershell
.\gradlew.bat build

```
if you want to test it 
``` powershell
./gradlew.bat runClient
```
Ready mod with Jar files are appearing into `build/libs`. To install mod you need Fabric API for Minecraft 1.21.11. 

Check recovery Java-logics are into `build`. You can run it:

```powershell
.\gradlew.bat recoveryChecks
```


If running Gradle on Windows with JDK 25 crashes due to Cyrillic characters in the temporary file path, set an existing temporary folder with a Latin path:

```powershell
New-Item -ItemType Directory -Force C:\JavaTemp | Out-Null
$env:JAVA_TOOL_OPTIONS = '-Djava.io.tmpdir=C:/JavaTemp -Djdk.net.unixdomain.tmpdir=C:/JavaTemp'
.\gradlew.bat runClient
```

Using cheat features on servers may violate server rules and terms of service, leading to a ban.