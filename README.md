# フロンティア社員情報管理システム

チームA / 社内向けの社員情報管理システム。社員検索・座席マップ・サンクスメッセージなどを提供する。

| | 技術 |
| --- | --- |
| フロントエンド | React 19 + TypeScript + Vite |
| バックエンド | Spring Boot 4 (Java 21) + Spring Data JPA + Spring Security |
| データベース | PostgreSQL 16 |
| 実行環境 | Docker Compose |

---

## 必要なもの

- **Docker Desktop**（これだけでOK。Java も Node もローカルには不要）
  - インストール後、Docker Desktop を**起動した状態**にしておくこと

---

## 初回セットアップ

### 1. リポジトリを取得する

```bash
git clone <このリポジトリのURL>
cd frontier
```

### 2. `.env` を作成する（**最重要・忘れやすい**）

`app/.env` には DB のパスワードなどを書く。**このファイルは Git にコミットされない**ため、
clone しただけでは存在しない。各自が自分の手元で作る必要がある。

```bash
cd app
cp .env.example .env
```

コピーするだけでよい。中身を書き換える必要はない（開発用の共通値が入っている）。

```
# app/.env
POSTGRES_USER=user
POSTGRES_PASSWORD=password
POSTGRES_DB=db
```

> **値は変更しないこと。**
> 変更すると既存の DB と食い違って接続できなくなる（下の「トラブルシューティング」参照）。

作成できたら、Git に含まれていないことを確認する。

```bash
git status
```

`.env` が一覧に出てこなければ正しい。`.env.example` はコミット対象なので出てきてよい。

### 3. 起動する

```bash
# app/ ディレクトリで実行する
docker compose up -d --build
```

初回はイメージのビルドで**5〜10分**かかる。2回目以降は `--build` なしで数十秒で起動する。

### 4. 起動を確認する

```bash
docker compose ps
```

3つとも `Up` になっていればOK（`db` は `Up (healthy)` になるまで10秒ほどかかる）。

```
NAME              SERVICE    STATUS
react-frontend    frontend   Up
spring-backend    backend    Up
spring-react-db   db         Up (healthy)
```

ブラウザで http://localhost:5174 を開き、画面が表示されれば完了。

---

## アクセス先

| 対象 | URL / 接続先 | 備考 |
| --- | --- | --- |
| フロントエンド | http://localhost:5174 | 開発はここを見る |
| バックエンドAPI | http://localhost:8081 | **401 が返るのが正常**（Spring Security の初期状態） |
| データベース | `localhost:5433` | DBクライアントから接続する場合。ユーザー/DB名はどちらも `.env` の値 |

> **ポート番号について**
> ホスト側とコンテナ内でポートが異なる。混同しやすいので注意。
> - ブラウザ・DBクライアントなど**手元のPCから** → `5174` / `8081` / `5433`
> - **コンテナ同士**（Vite のプロキシ設定など） → `frontend:5173` / `backend:8080` / `db:5432`

フロントから API を叩くときは `fetch('/api/employees')` と書く。
Vite が `backend:8080` に転送するので、CORS の設定は不要（[app/frontend/vite.config.ts](app/frontend/vite.config.ts) を参照）。

---

## よく使うコマンド

すべて `app/` ディレクトリで実行する。

```bash
docker compose up -d              # 起動
docker compose down               # 停止（DBのデータは残る）
docker compose ps                 # 状態確認
docker compose logs -f backend    # ログを見る（backend / frontend / db）
docker compose up -d --build      # コードを変えた後に作り直す
```

- **フロントエンド**：ソースを保存すると自動で反映される（ホットリロード）。再起動不要
- **バックエンド**：Java のコードを変えたら `docker compose up -d --build backend` が必要

### DB に直接繋ぐ

```bash
docker exec -it spring-react-db psql -U user -d db
```

`\dt` でテーブル一覧、`\q` で終了。

---

## トラブルシューティング

### `.env` が無い、というエラーで起動しない

セットアップ手順 2 を実行していない。`cd app && cp .env.example .env` を実行する。

### DB に接続できない / 認証エラーが出る

`.env` の `POSTGRES_USER` や `POSTGRES_PASSWORD` を書き換えた場合に起きる。

PostgreSQL は**ボリュームが空の初回起動時にしかユーザーとパスワードを作らない**ため、
`.env` だけ変えても DB 側は古い値のまま残っている。

直すにはボリュームごと作り直す。**DB のデータは全部消える。**

```bash
docker compose down -v      # -v でボリュームも削除
docker compose up -d
```

### ポートが既に使われている、と言われる

`5174` / `8081` / `5433` のいずれかを他のアプリが使っている。
使用中のプロセスを止めるか、[app/docker-compose.yml](app/docker-compose.yml) の `ports` の**左側**の数字を変更する
（右側は変更しないこと）。

### とりあえず全部作り直したい

```bash
docker compose down -v
docker compose up -d --build
```

---

## ディレクトリ構成

```
frontier/
├── app/
│   ├── .env                 # 各自が作成（Git管理外）
│   ├── .env.example         # .env のひな形（Git管理）
│   ├── docker-compose.yml
│   ├── backend/             # Spring Boot
│   │   └── src/main/java/jp/co/frontier/ems/
│   └── frontend/            # React + Vite
│       └── src/
├── 要求定義書.md
├── 機能要件.md
├── API設計仕様書.md
└── プロジェクト計画書テンプレート.md
```

---

## ドキュメント

| ファイル | 内容 |
| --- | --- |
| [要求定義書.md](要求定義書.md) | 目的・背景、ターゲット、ユースケース、業務フロー |
| [機能要件.md](機能要件.md) | 機能一覧、画面一覧、非機能要件 |
| [API設計仕様書.md](API設計仕様書.md) | エンドポイント定義、共通エラーコード、認証方式 |
| [プロジェクト計画書テンプレート.md](プロジェクト計画書テンプレート.md) | スケジュール、体制 |
