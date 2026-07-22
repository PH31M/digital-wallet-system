.PHONY: help build up down restart logs ps clean test db-shell backend-shell db-reset

help: ## Hiển thị danh sách command
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-15s\033[0m %s\n", $$1, $$2}'

build: ## Build lại image backend
	docker compose build

up: ## Khởi động toàn bộ services (chạy nền)
	docker compose up -d

down: ## Dừng và xóa containers
	docker compose down

restart: down up ## Restart toàn bộ services

logs: ## Xem log backend realtime
	docker compose logs -f backend

logs-all: ## Xem log tất cả services
	docker compose logs -f

ps: ## Xem trạng thái containers
	docker compose ps

clean: ## Dừng containers và xóa luôn volume (mất data DB)
	docker compose down -v

test: ## Chạy unit test trong container build tạm
	docker compose run --rm backend mvn test

db-shell: ## Vào psql shell của container postgres
	docker compose exec postgres psql -U $${POSTGRES_USER:-postgres} -d $${POSTGRES_DB:-digital_wallet}

db-reset: ## Xóa và tạo lại database (mất data)
	docker compose exec postgres psql -U $${POSTGRES_USER:-postgres} -c "DROP DATABASE IF EXISTS $${POSTGRES_DB:-digital_wallet};"
	docker compose exec postgres psql -U $${POSTGRES_USER:-postgres} -c "CREATE DATABASE $${POSTGRES_DB:-digital_wallet};"

backend-shell: ## Vào shell container backend
	docker compose exec backend sh
