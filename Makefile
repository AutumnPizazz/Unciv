# UncivCN 构建与开发便捷入口：make <target>
# 每个目标名后的 ## 说明会被 make help 自动列出。

.DEFAULT_GOAL := help

.PHONY: help build test check run dist docs docs-site detekt bump clean

help: ## 显示本帮助
	@awk 'BEGIN {FS = ":.*## "} /^[a-zA-Z0-9_-]+:.*## / {printf "  %-16s %s\n", $$1, $$2}' $(MAKEFILE_LIST) | sort

build: ## 编译所有模块（./gradlew classes）
	./gradlew classes

test: ## 运行全部测试（./gradlew :tests:test）
	./gradlew :tests:test

check: ## 运行全部检查（./gradlew check）
	./gradlew check

run: ## 运行桌面版（./gradlew desktop:run）
	./gradlew desktop:run

dist: ## 打包桌面版 JAR（./gradlew desktop:dist）
	./gradlew desktop:dist

docs: ## 生成游戏文档（./gradlew desktop:generateDocs）
	./gradlew desktop:generateDocs

docs-site: ## 构建 VitePress 文档站（docs-vitepress）
	cd docs-vitepress && npm ci && npm run docs:build

detekt: ## Detekt 静态检查（CLI，配置见 docs/zh/UncivCN/Coding-standards.md）
	java -jar detekt-cli.jar --parallel --report html:detekt/reports.html \
		--config .github/workflows/detekt_config/detekt-warnings.yml

bump: ## 升版本（./gradlew bumpVersion；make bump newVersion=x 或 make bump bump=patch）
	./gradlew bumpVersion $(if $(newVersion),-PnewVersion=$(newVersion)) $(if $(bump),-Pbump=$(bump))

clean: ## 清理构建产物（./gradlew clean）
	./gradlew clean
