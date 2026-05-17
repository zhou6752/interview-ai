# PostgreSQL 16 启动脚本
# 运行方式：右键 → 使用 PowerShell 运行，或管理员终端执行 .\start-postgres.ps1

$PGROOT = "D:\Program Files\PostgreSQL\16"
$PGDATA = "D:\pgdata"

# 检查数据目录是否已初始化
if (-not (Test-Path "$PGDATA\PG_VERSION")) {
    Write-Host "首次运行，正在初始化数据库..."
    & "$PGROOT\bin\initdb.exe" -D $PGDATA -U postgres -E UTF8 --locale=C
}

# 启动服务
Write-Host "正在启动 PostgreSQL..."
$env:PATH = "$PGROOT\bin;$env:PATH"
& "$PGROOT\bin\pg_ctl.exe" -D $PGDATA -l "$PGDATA\pg.log" start

Write-Host "PostgreSQL 已启动 (端口 5432)"
Write-Host "连接方式: $PGROOT\bin\psql.exe -U postgres"