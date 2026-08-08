#!/usr/bin/env python3
"""UncivCN 文档站本地预览服务器。

- 服务 docs-vitepress/.vitepress/dist，映射 /Unciv/ 前缀（与 GitHub Pages 路径一致）
- 无扩展名路径自动补 .html（如 /zh/UncivCN/新特性 -> 新特性.html）
- 空闲超时（默认 5 分钟无 HTTP 访问）自动退出，Ctrl+C 立即退出
- 关闭浏览器后无需手动清理进程

用法：python scripts/preview_server.py [--port 4173] [--idle-timeout 300]
"""
import argparse
import http.server
import os
import sys
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent  # docs-vitepress/
DIST = ROOT / '.vitepress' / 'dist'

BASE = '/Unciv/'
last_access = time.time()


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(DIST), **kwargs)

    def translate_path(self, path: str) -> str:
        # /Unciv/xxx -> /xxx（去掉 base 前缀）
        if path == BASE.rstrip('/') or path == BASE:
            path = '/'
        elif path.startswith(BASE):
            path = path[len(BASE) - 1:]  # 保留前导 /
        return super().translate_path(path)

    def _serve(self):
        # 无扩展名的路径自动尝试 .html（保留 query）
        if '?' in self.path:
            base, query = self.path.split('?', 1)
        else:
            base, query = self.path, ''
        p = self.translate_path(base)
        if not os.path.exists(p) and not base.endswith('/') and '.' not in base.rsplit('/', 1)[-1]:
            self.path = base + '.html' + (('?' + query) if query else '')
        super().do_GET()

    def do_GET(self):
        self._serve()

    def do_HEAD(self):
        self._serve()

    def log_message(self, fmt: str, *args):
        global last_access
        last_access = time.time()
        print(f"[{time.strftime('%H:%M:%S')}] {self.address_string()} {fmt % args}", flush=True)


def main() -> int:
    global last_access
    parser = argparse.ArgumentParser(description='UncivCN 文档站本地预览服务器')
    parser.add_argument('--port', type=int, default=4173)
    parser.add_argument('--idle-timeout', type=int, default=300,
                        help='无 HTTP 访问多少秒后自动退出（默认 300）')
    args = parser.parse_args()

    if not DIST.exists():
        print(f'[错误] 未找到构建产物 {DIST}，请先运行 build.bat 构建。')
        return 1

    try:
        server = http.server.ThreadingHTTPServer(('127.0.0.1', args.port), Handler)
    except OSError as e:
        print(f'[错误] 端口 {args.port} 启动失败：{e}')
        print('      可能是已有预览服务器在运行（直接访问 http://localhost:4173/Unciv/ 即可）。')
        return 1

    print(f'预览地址: http://localhost:{args.port}/Unciv/')
    print(f'空闲 {args.idle_timeout} 秒无访问将自动退出；Ctrl+C 立即停止。')
    print('关闭本窗口或浏览器后无需手动清理进程。')

    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        while True:
            time.sleep(2)
            if time.time() - last_access > args.idle_timeout:
                print('[提示] 已空闲超时，自动退出。')
                break
    except KeyboardInterrupt:
        print('\n[提示] 已手动停止。')
    finally:
        server.shutdown()
        server.server_close()
    return 0


if __name__ == '__main__':
    sys.exit(main())
