powershell (Get-FileHash target\willow-deployer-*-jar-with-dependencies.jar -Algorithm MD5).hash.ToLower(); > md5.txt
SET /P MD5= < md5.txt
DEL md5.txt
powershell "(Get-Content src\main\shell\deployer.sh).count + 1" > lines.txt
SET /P ARCHIVE_START= < lines.txt
DEL lines.txt
powershell "Get-Content src\main\shell\deployer.sh | %%{$_ -replace '@@MD5@@', $env:MD5} | %%{$_ -replace '@@ARCHIVE_START@@', $env:ARCHIVE_START}" > target\deployer.tmp
COPY /B target\deployer.tmp + target\willow-deployer-*-jar-with-dependencies.jar target\deployer.sh

powershell "(Get-Content src\main\shell\deployer.cmd).count" > lines.txt
SET /P BATCH_LINES= < lines.txt
DEL lines.txt
powershell "Get-Content src\main\shell\deployer.cmd | %%{$_ -replace '@@MD5@@', $env:MD5} | %%{$_ -replace '@@BATCH_LINES@@', $env:BATCH_LINES}" > target\deployer.tmp
COPY /B target\deployer.tmp + target\willow-deployer-*-jar-with-dependencies.jar target\deployer.cmd
DEL target\deployer.tmp

COPY target\willow-deployer-*-jar-with-dependencies.jar target\deployer-uber-%MD5%.jar
