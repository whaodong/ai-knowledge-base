#!/bin/bash

echo "==================================="
echo "AI知识库前端项目构建脚本"
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

echo "安装依赖..."
$PKG_MANAGER install
echo ""

echo "运行代码检查..."
$PKG_MANAGER run lint
echo ""

echo "构建项目..."
$PKG_MANAGER run build
echo ""

echo "==================================="
echo "构建完成!"
echo "输出目录: .next/"
echo "==================================="
