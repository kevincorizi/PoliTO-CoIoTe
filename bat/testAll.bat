@echo off
for /f %%f in ('dir /b ..\input\') do (
echo Testing feasibility of %%f...
java -classpath ../bin/;. com.kamjae.coiote.Main -jar coiote.jar -i ../input/%%f -test
)
pause