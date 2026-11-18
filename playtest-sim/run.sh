#!/usr/bin/env bash
#
# 启动一次本地双客户端同时回合试玩模拟（A/B 两个客户端 + 一次结算）。
#
# 所有产物集中写入 .local/playtest-sim.*：
#   server/          真实 server-go 的 SQLite 与日志（real 模式）
#   clients/clientA/ 每回合上传的操作、下载的存档与状态指纹
#   clients/clientB/
#   logs/report.md   可读的过程报告
#
# 用法：
#   ./playtest-sim/run.sh              # 默认：用测试内置的 HttpServer，不依赖 Go
#   PLAYTEST_PORT=12000 ./playtest-sim/run.sh real  # 仅在有本地私有 server-go/ 副本时可用
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-embedded}"
SRV="$ROOT/server-go"
case "$MODE" in
  embedded) ;;
  real)
    if [[ ! -f "$SRV/go.mod" ]]; then
      echo "[playtest] real 模式需要已获授权的本地 server-go/ 副本。公开仓库请使用 embedded 模式。" >&2
      exit 1
    fi
    ;;
  *)
    echo "用法：$0 [embedded|real]" >&2
    exit 2
    ;;
esac

mkdir -p "$ROOT/.local"
SIM="$(mktemp -d "$ROOT/.local/playtest-sim.XXXXXX")"
PORT="${PLAYTEST_PORT:-11451}"

mkdir -p "$SIM/server" "$SIM/logs" "$SIM/clients"

SERVER_PID=""
cleanup() {
  if [[ -n "$SERVER_PID" ]]; then
    kill "$SERVER_PID" 2>/dev/null || true
    wait "$SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

if [[ "$MODE" == "embedded" ]]; then
  echo "[playtest] embedded mode: the test will boot its own in-process HTTP server"
  echo "server=" > "$SIM/config.properties"
else
  echo "[playtest] building server-go"
  (cd "$SRV" && go build -o "$SIM/server/unciv-srv" .)
  echo "[playtest] starting server-go on port $PORT"
  (
    cd "$SIM/server"
    PORT="$PORT" \
    DB_PATH="$SIM/server/unciv-srv.db" \
    ADMIN_PASSWORD="$(openssl rand -hex 24)" \
    REGISTER_MODE=open \
    CHAT_ENABLED=false \
    exec "$SIM/server/unciv-srv"
  ) > "$SIM/server/server.log" 2>&1 &
  SERVER_PID=$!

  READY=0
  for _ in $(seq 1 60); do
    if curl -sf "http://127.0.0.1:$PORT/isalive" >/dev/null 2>&1; then READY=1; break; fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      echo "[playtest] server exited early; see $SIM/server/server.log" >&2
      tail -40 "$SIM/server/server.log" >&2 || true
      exit 1
    fi
    sleep 0.5
  done
  if [[ "$READY" != "1" ]]; then
    echo "[playtest] server did not become ready in time" >&2
    exit 1
  fi
  echo "server=http://127.0.0.1:$PORT" > "$SIM/config.properties"
  echo "[playtest] server is up at http://127.0.0.1:$PORT"
fi

cd "$ROOT"
./gradlew :tests:test \
  "-Dplaytest.sim.dir=$SIM" \
  --tests "com.unciv.logic.multiplayer.SimultaneousTurnPlaytestSimulation" \
  --tests "com.unciv.logic.multiplayer.RestartVoteIntegrationTest"

echo
echo "[playtest] done."
echo "  report:   $SIM/logs/report.md"
echo "  log:      $SIM/logs/simulation.log"
echo "  clients:  $SIM/clients/{clientA,clientB}"
