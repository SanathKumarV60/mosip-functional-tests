rem FOR /F "delims=" %i IN ('cd') DO set curwd=%i
rem echo %cwd%
start Simulator.exe
java -jar mosip-packet-creator-0.0.1-SNAPSHOT.jar --spring.config.location=file:///C:\Mosip.io\gitrepos\mosip-functional-tests\packet-utility\deploy\config\application.properties