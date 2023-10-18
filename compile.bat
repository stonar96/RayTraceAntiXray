@echo off
cd "%~dp0"
call mvn clean package
PAUSE
