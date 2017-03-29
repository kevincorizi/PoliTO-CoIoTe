@echo off
echo Compiling...
javac -classpath . -d ../bin/ -sourcepath src ../src/com/kamjae/coiote/*.java
pause