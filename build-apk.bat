@echo off
chcp 65001 >nul
echo ========================================
echo 星织AI - APK打包工具
echo ========================================
echo.

REM 设置Java环境（自动检测）
if exist "D:\java\bin\java.exe" (
    set JAVA_HOME=D:\java
) else if exist "C:\Program Files\Java\jdk-17\bin\java.exe" (
    set JAVA_HOME=C:\Program Files\Java\jdk-17
) else if exist "C:\Program Files\Java\jdk-11\bin\java.exe" (
    set JAVA_HOME=C:\Program Files\Java\jdk-11
) else (
    echo 警告：未找到Java，使用系统默认配置
)

echo [1/3] 清理旧文件...
call gradlew.bat clean
if errorlevel 1 (
    echo 清理失败！请检查Java环境配置
    pause
    exit /b 1
)

echo.
echo [2/3] 开始构建Debug APK...
call gradlew.bat assembleDebug
if errorlevel 1 (
    echo 构建失败！请查看错误信息
    pause
    exit /b 1
)

echo.
echo [3/3] 构建完成！
echo.
echo ========================================
echo APK文件位置：
echo %cd%\app\build\outputs\apk\debug\app-debug.apk
echo ========================================
echo.
echo 您可以：
echo 1. 将APK文件复制到手机安装
echo 2. 使用 adb install 命令安装
echo 3. 通过微信/QQ发送给朋友
echo.

REM 尝试打开APK所在文件夹
if exist "app\build\outputs\apk\debug\" (
    echo 正在打开APK文件夹...
    explorer "app\build\outputs\apk\debug\"
)

pause
