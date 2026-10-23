# GitHub 上传前清理脚本
# 此脚本将删除不必要的文件和敏感信息

Write-Host "🧹 开始清理项目..." -ForegroundColor Green

# 1. 删除开发过程中的临时文档
Write-Host "`n📝 删除开发文档..." -ForegroundColor Yellow

$devDocs = @(
    "CHANGELOG_v1.1.0.md",
    "CHANGELOG_v1.2.0.md",
    "AI大脑集成示例.kt",
    "ApiHandler集成示例.md",
    "DeepSeek集成说明.md",
    "KnoxChat方案对比与改进总结.md",
    "NEW_PROMPT_TEMPLATE.txt",
    "PROJECT_EXPERIENCE.md",
    "replace_prompt.ps1",
    "改进建议.docx",
    "验证代码完整性.bat",
    "验证编译.bat",
    "如何查看崩溃日志.md",
    "无限上下文增强使用指南.md"
)

foreach ($doc in $devDocs) {
    $path = Join-Path $PSScriptRoot $doc
    if (Test-Path $path) {
        Remove-Item $path -Force
        Write-Host "  ✓ 已删除: $doc" -ForegroundColor Gray
    }
}

# 2. 删除所有带表情符号的开发报告文档
Write-Host "`n📄 删除开发报告文档..." -ForegroundColor Yellow

$emojiDocs = Get-ChildItem -Path $PSScriptRoot -Filter "*.md" -File | 
    Where-Object { $_.Name -match "^[✅🎯📊📖📝📱🔍🔒🔧🚀🧪🎉🎨💰]" }

foreach ($doc in $emojiDocs) {
    Remove-Item $doc.FullName -Force
    Write-Host "  ✓ 已删除: $($doc.Name)" -ForegroundColor Gray
}

# 3. 删除备份文件
Write-Host "`n🗑️ 删除备份文件..." -ForegroundColor Yellow

$backupFiles = Get-ChildItem -Path $PSScriptRoot -Include "*.backup","*.bak","*.tmp" -File -Recurse | 
    Where-Object { $_.FullName -notmatch "\.gradle|build" }

foreach ($file in $backupFiles) {
    Remove-Item $file.FullName -Force
    Write-Host "  ✓ 已删除备份: $($file.Name)" -ForegroundColor Gray
}

# 4. 删除不必要的图片
Write-Host "`n🖼️ 删除测试图片..." -ForegroundColor Yellow

$testImages = @(
    "xingzhi.png",
    "秋岚.jpg",
    "默认图标.png",
    "默认角色.png"
)

foreach ($img in $testImages) {
    $path = Join-Path $PSScriptRoot $img
    if (Test-Path $path) {
        Remove-Item $path -Force
        Write-Host "  ✓ 已删除: $img" -ForegroundColor Gray
    }
}

# 5. 删除 .idea 目录 (IDE配置)
Write-Host "`n💻 删除IDE配置..." -ForegroundColor Yellow

$ideaPath = Join-Path $PSScriptRoot ".idea"
if (Test-Path $ideaPath) {
    Remove-Item $ideaPath -Recurse -Force
    Write-Host "  ✓ 已删除: .idea 目录" -ForegroundColor Gray
}

# 6. 删除 .gradle 目录
Write-Host "`n📦 删除Gradle缓存..." -ForegroundColor Yellow

$gradlePath = Join-Path $PSScriptRoot ".gradle"
if (Test-Path $gradlePath) {
    Remove-Item $gradlePath -Recurse -Force
    Write-Host "  ✓ 已删除: .gradle 目录" -ForegroundColor Gray
}

# 7. 删除 build 目录
Write-Host "`n🏗️ 删除构建输出..." -ForegroundColor Yellow

$buildPath = Join-Path $PSScriptRoot "app\build"
if (Test-Path $buildPath) {
    Remove-Item $buildPath -Recurse -Force
    Write-Host "  ✓ 已删除: app\build 目录" -ForegroundColor Gray
}

Write-Host "`n✅ 清理完成！" -ForegroundColor Green
Write-Host "`n📋 保留的重要文件:" -ForegroundColor Cyan
Write-Host "  • README.md - 项目说明文档" -ForegroundColor White
Write-Host "  • 功能说明.md - 功能列表" -ForegroundColor White
Write-Host "  • .gitignore - Git忽略规则" -ForegroundColor White
Write-Host "  • 所有源代码文件 (*.kt)" -ForegroundColor White
Write-Host "  • 所有资源文件 (res/, assets/)" -ForegroundColor White
Write-Host "  • Gradle配置文件 (build.gradle, settings.gradle)" -ForegroundColor White
Write-Host "`n⚠️  注意: 请确保已检查代码中是否还有硬编码的API密钥！" -ForegroundColor Yellow
