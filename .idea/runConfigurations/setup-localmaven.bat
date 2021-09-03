@echo off

echo Setting up required local maven packages
echo.

echo Paper 1.16.R3 nms
echo Running setup-localhost...
cmd /c .idea\runConfigurations\setup-localhost.bat

echo.
echo Installing Paper 1.16.R3 nms...
java -Dpaperclip.install=true -jar run\server-1\server.jar
echo Finished!