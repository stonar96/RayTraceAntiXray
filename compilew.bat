@echo off
cd "%~dp0"
call mvnw.cmd clean package
PAUSE
