@echo off
type nul > ../output/summary.csv
for /f %%f in ('dir /b ..\input\') do (
echo Printing complete solution for %%f...
java -classpath ../bin/;. com.kamjae.coiote.Main -jar coiote.jar -i ../input/%%f -o ../output/summary.csv -s ../solutions/Sol_%%f
)
pause