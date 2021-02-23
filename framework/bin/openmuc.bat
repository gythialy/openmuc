::BATCH file for windows

set BATDIR=%~dp0
set CURRENTDIR=%cd%

call %BATDIR%..\..\gradlew.bat -b %BATDIR%..\conf\bundles.conf.gradle updateBundles --warning-mode all

cd %BATDIR%..\
java -jar felix\felix.jar
cd %CURRENTDIR%
