@ECHO OFF
SETLOCAL

SET MD5=@@MD5@@

IF NOT "%1" == "-d" GOTO nodebug
SHIFT
SET DEBUG_PORT=4444
IF "%1" == "" GOTO defaultport
IF %1 EQU +%1 (
    SET DEBUG_PORT=%1
    SHIFT
)
:defaultport
SET DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=%DEBUG_PORT% 
:nodebug

IF NOT DEFINED W_DEPLOYER_HOME (
  SET W_DEPLOYER_HOME=%~p0
  SET W_DEPLOYER_LIB=%~p0lib
)

IF NOT EXIST "%W_DEPLOYER_LIB%" (
  mkdir "%W_DEPLOYER_LIB%"
  IF NOT EXIST "%W_DEPLOYER_LIB%\" (
    ECHO Failed to create deployer lib directory
    EXIT /B 1
  )
)

SET W_DEPLOYER_JAR=%W_DEPLOYER_LIB%\deployer-uber-%MD5%.jar
powershell do { New-Item .lock -type directory; $s = $?; if (-not $s) { Start-Sleep -s 1; } } until ($s); > NUL
IF NOT EXIST "%W_DEPLOYER_JAR%" powershell $bytes = [System.IO.File]::ReadAllBytes(\"%~f0\"); $count = 0; for ($i = 0; $i -lt $bytes.length; $i++) { if ($bytes[$i] -eq 10 -and ++$count -eq @@BATCH_LINES@@) { [System.IO.File]::WriteAllBytes(\"%W_DEPLOYER_JAR%\", $bytes[($i + 1)  .. ($bytes.length - 1)]); exit; } }
rmdir .lock

IF NOT DEFINED JAVA_HOME (
  SET JAVA_EXE= & FOR /F %%I IN ("java.exe") DO SET JAVA_EXE=%%~$PATH:I
  IF NOT DEFINED JAVA_EXE (
    ECHO No java or java on PATH
    EXIT /B 1
  ) ELSE (
    java -cp "%W_DEPLOYER_JAR%" com.nitorcreations.willow.deployer.JavaHome > tmp.txt
    SET /P JAVA_HOME= < tmp.txt
    DEL tmp.txt
  )
)
IF EXIST "%JAVA_HOME%\..\lib\" (
  SET JAVA_LIB=%JAVA_HOME%\..\lib
) ELSE IF EXIST "%JAVA_HOME%\lib\" (
  SET JAVA_LIB=%JAVA_HOME%\lib
) ELSE (
  ECHO Could not find java lib dir
  EXIT /B 2
)

SET JAVA_TOOLS=%JAVA_LIB%\tools.jar
SET W_DEPLOYER_NAME=%2

IF "%1" == "start" (
  "%JAVA_HOME%\bin\java" %DEBUG% -cp "%JAVA_TOOLS%;%W_DEPLOYER_JAR%" com.nitorcreations.willow.deployer.Main %2 %3 %4 %5 %6 %7 %8 %9 2>&1
  EXIT /B
) ELSE IF "%1" == "stop" (
  "%JAVA_HOME%\bin\java" %DEBUG% -cp "%JAVA_TOOLS%;%W_DEPLOYER_JAR%" com.nitorcreations.willow.deployer.Stop %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B
) ELSE IF "%1" == "list" (
  "%JAVA_HOME%\bin\java" %DEBUG% -cp "%JAVA_TOOLS%;%W_DEPLOYER_JAR%" com.nitorcreations.willow.deployer.GetList %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B
) ELSE IF "%1" == "status" (
  "%JAVA_HOME%\bin\java" %DEBUG% -cp "%JAVA_TOOLS%;%W_DEPLOYER_JAR%" com.nitorcreations.willow.deployer.Status %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B
) ELSE IF "%1" == "restartchild" (
  "%JAVA_HOME%\bin\java" %DEBUG% -cp "%JAVA_TOOLS%;%W_DEPLOYER_JAR%" com.nitorcreations.willow.deployer.RestartChild %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B
) ELSE IF "%1" == "jmxoperation" (
  "%JAVA_HOME%\bin\java" %DEBUG% -cp "%JAVA_TOOLS%;%W_DEPLOYER_JAR%" com.nitorcreations.willow.deployer.JMXOperation %2 %3 %4 %5 %6 %7 %8 %9
  EXIT /B
) ELSE (
  ECHO usage %0 {start^|stop^|list^|status^|restartchild^|jmxoperation} [role] url [url [...]]
  EXIT /B 1
)

ENDLOCAL
EXIT /B
