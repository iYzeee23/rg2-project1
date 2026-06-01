@echo off
REM ============================================================
REM  build.bat - Kompajlira projekat Simulacija planete Zemlje
REM  Pokrenuti iz korenog foldera projekta: build.bat
REM ============================================================

echo [BUILD] Kompajliranje Java fajlova...

if not exist "build" mkdir build

javac -encoding UTF-8 ^
      -cp "lib/jogl-all.jar;lib/gluegen-rt.jar;lib/joml-1.8.5-SNAPSHOT.jar" ^
      -d build ^
      src\*.java

if %ERRORLEVEL% NEQ 0 (
    echo [BUILD] GRESKA pri kompajliranju!
    pause
    exit /b 1
)

echo [BUILD] Kompajliranje uspesno! Klase su u build/ folderu.
echo [BUILD] Pokrenite: run.bat
