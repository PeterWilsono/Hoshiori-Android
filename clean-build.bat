@echo off
echo 清理Gradle缓存和构建文件...

echo.
echo [1/4] 清理build目录...
if exist build rmdir /s /q build
if exist app\build rmdir /s /q app\build

echo.
echo [2/4] 清理.gradle目录...
if exist .gradle rmdir /s /q .gradle

echo.
echo [3/4] 清理Gradle缓存...
.\gradlew.bat clean

echo.
echo [4/4] 重新构建项目...
.\gradlew.bat build

echo.
echo ✅ 清理和构建完成！
pause
