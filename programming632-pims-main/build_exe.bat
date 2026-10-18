@echo off
REM Builds the Windows executable. Needs JDK 17+ (jpackage). Change APPNAME to yourname_pims
set APPNAME=yourname_pims
call build.bat
if exist dist rmdir /S /Q dist
mkdir dist
jpackage --type app-image --name %APPNAME% --input . --main-jar pims.jar --main-class pims.Main --dest dist --win-console
copy /Y database.sql dist\%APPNAME%\ >nul
copy /Y db.properties dist\%APPNAME%\ >nul
echo.
echo Done: dist\%APPNAME%\%APPNAME%.exe   (zip the whole dist\%APPNAME% folder)
pause
