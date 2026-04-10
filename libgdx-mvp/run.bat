@echo off
REM Wrapper so you don't need to fix PATH in Cursor: finds JDK and runs Gradle.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run.ps1" %*
