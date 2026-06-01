@echo off
REM ============================================================
REM  run.bat - Pokrece Simulaciju planete Zemlje
REM  Pokrenuti iz project/ foldera: run.bat
REM ============================================================

echo [RUN] Pokretanje EarthSimulation...

java -cp "build;lib/jogl-all.jar;lib/gluegen-rt.jar;lib/joml-1.8.5-SNAPSHOT.jar;lib/jogl-all-natives-windows-amd64.jar;lib/gluegen-rt-natives-windows-amd64.jar" ^
     project.EarthSimulation

if %ERRORLEVEL% NEQ 0 (
    echo [RUN] Program se zavrsio sa greskom.
    pause
)
