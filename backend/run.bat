@echo off
chcp 65001 >nul

if not exist target mkdir target

echo Compiling...
javac -d target/classes -sourcepath src/main/java -cp "libs/*" src/main/java/com/lifeshare/LifeShareApplication.java src/main/java/com/lifeshare/config/*.java src/main/java/com/lifeshare/controller/*.java src/main/java/com/lifeshare/dto/*.java src/main/java/com/lifeshare/entity/*.java src/main/java/com/lifeshare/repository/*.java src/main/java/com/lifeshare/service/*.java src/main/java/com/lifeshare/service/impl/*.java src/main/java/com/lifeshare/strategy/*.java src/main/java/com/lifeshare/strategy/impl/*.java 2>&1

if %errorlevel% neq 0 (
    echo Compilation failed!
    exit /b 1
)

echo Running...
java -cp "target/classes;libs/*" com.lifeshare.LifeShareApplication
