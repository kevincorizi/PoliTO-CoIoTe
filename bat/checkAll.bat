@echo off
for /f %%f in ('dir /b ..\input\') do (
echo Checking the gap for %%f...
java -classpath ../bin/;. com.kamjae.coiote.Main -jar coiote.jar -i ../input/%%f -os ../material/optimal_solutions.csv
)
pause