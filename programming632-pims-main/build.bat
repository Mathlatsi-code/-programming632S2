@echo off
REM Compiles the source and builds pims.jar (needs JDK 11+; add mysql-connector-j to lib\ if you use MySQL 8)
if not exist out mkdir out
javac -encoding UTF-8 -cp "lib\*" -d out src\pims\*.java
copy /Y database.sql out\database.sql >nul
echo Main-Class: pims.Main> manifest.txt
echo Class-Path: lib/mariadb-java-client.jar lib/mysql-connector-j.jar>> manifest.txt
jar cfm pims.jar manifest.txt -C out .
del manifest.txt
echo Built pims.jar
