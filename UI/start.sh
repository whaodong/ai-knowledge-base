#!/bin/bash

echo "==================================="
echo "AI知识库前端项目启动脚本"
echo "==================================="

# 检查Node.js是否安装
if ! command -v node &> /dev/null; then
    echo "错误: Node.js 未安装，请先安装 Node.js 18+"
    exit 1
fi

echo "Node.js 版本: $(node -v)"
echo ""

# 检查包管理器
if command -v pnpm &> /dev/null; then
    PKG_MANAGER="pnpm"
elif command -v npm &> /dev/null; then
    PKG_MANAGER="npm"
else
    echo "错误: 未找到包管理器 (npm/pnpm)"
    exit 1
fi

echo "使用包管理器: $PKG_MANAGER"
echo ""

# 检查node_modules是否存在
if [ ! -d "node_modules" ]; then
    echo "安装依赖..."
    $PKG_MANAGER install
    echo ""
fi

# 检查.env.local文件
if [ ! -f ".env.local" ]; then
    echo "创建 .env.local 文件..."
    cat > .env.local << EOL
NEXT_PUBLIC_API_URL=http://localhost:8080
NEXT_PUBLIC_WS_URL=ws://localhost:8080
EOL
    echo ""
fi

echo "启动开发服务器..."
echo "访问地址: http://localhost:3000"
echo ""

$PKG_MANAGER run dev
