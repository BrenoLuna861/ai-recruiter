# ============================================================
# AI Recruiter - Makefile
# Comandos rápidos para desenvolvimento e deploy
# ============================================================

.PHONY: help dev prod down logs clean build test setup

# Cores para output
GREEN  := \033[0;32m
YELLOW := \033[0;33m
CYAN   := \033[0;36m
RESET  := \033[0m

help: ## Mostra esta ajuda
	@echo ""
	@echo "$(CYAN)🧠 AI Recruiter - Comandos disponíveis:$(RESET)"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-20s$(RESET) %s\n", $$1, $$2}'
	@echo ""

setup: ## Configura o ambiente inicial (copia .env.example)
	@if [ ! -f .env ]; then \
		cp .env.example .env; \
		echo "$(YELLOW)⚠️  Edite o arquivo .env com suas variáveis (especialmente ANTHROPIC_API_KEY)$(RESET)"; \
	else \
		echo "$(GREEN)✅ .env já existe$(RESET)"; \
	fi

dev: ## Sobe apenas MySQL e MongoDB para desenvolvimento local
	@echo "$(CYAN)🚀 Subindo banco de dados para desenvolvimento...$(RESET)"
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d mysql mongodb
	@echo ""
	@echo "$(GREEN)✅ Bancos prontos!$(RESET)"
	@echo "   MySQL:   localhost:3306"
	@echo "   MongoDB: localhost:27017"
	@echo ""
	@echo "$(YELLOW)Agora rode em terminais separados:$(RESET)"
	@echo "  Backend:  cd backend && mvn spring-boot:run"
	@echo "  Frontend: cd frontend && npm run dev"

prod: ## Sobe o stack completo em produção
	@echo "$(CYAN)🚀 Subindo stack completo...$(RESET)"
	@if [ ! -f .env ]; then echo "$(YELLOW)⚠️  Crie o arquivo .env primeiro: make setup$(RESET)"; exit 1; fi
	docker-compose up -d --build
	@echo "$(GREEN)✅ Aplicação no ar em http://localhost$(RESET)"

down: ## Para todos os containers
	docker-compose down

down-volumes: ## Para containers E apaga os volumes (CUIDADO: apaga dados!)
	@read -p "$(YELLOW)⚠️  Isso apagará todos os dados. Continuar? [y/N] $(RESET)" confirm; \
	if [ "$$confirm" = "y" ]; then docker-compose down -v; echo "$(GREEN)Volumes apagados$(RESET)"; fi

logs: ## Ver logs de todos os serviços
	docker-compose logs -f

logs-backend: ## Ver logs apenas do backend
	docker-compose logs -f backend

logs-frontend: ## Ver logs apenas do frontend
	docker-compose logs -f frontend

build: ## Rebuild das imagens Docker
	docker-compose build --no-cache

test-backend: ## Roda os testes do backend
	@echo "$(CYAN)🧪 Rodando testes do backend...$(RESET)"
	cd backend && mvn test -B

build-frontend: ## Build do frontend para produção
	@echo "$(CYAN)🔨 Building frontend...$(RESET)"
	cd frontend && npm ci && npm run build

install-frontend: ## Instala dependências do frontend
	cd frontend && npm install

ps: ## Status dos containers
	docker-compose ps

restart-backend: ## Reinicia apenas o backend
	docker-compose restart backend

mysql-cli: ## Acessa o MySQL via CLI
	docker-compose exec mysql mysql -u airecruiter -p ai_recruiter

mongo-cli: ## Acessa o MongoDB via CLI
	docker-compose exec mongodb mongosh ai_recruiter_logs

backup-db: ## Faz backup do MySQL
	@echo "$(CYAN)💾 Fazendo backup do MySQL...$(RESET)"
	docker-compose exec mysql mysqldump -u airecruiter -p ai_recruiter > backup_$(shell date +%Y%m%d_%H%M%S).sql
	@echo "$(GREEN)✅ Backup criado$(RESET)"

clean: ## Remove containers, imagens e volumes não utilizados
	docker system prune -f
	docker volume prune -f

health: ## Verifica saúde da aplicação
	@echo "$(CYAN)🔍 Verificando saúde...$(RESET)"
	@curl -sf http://localhost/api/auth/health && echo "$(GREEN)✅ API: OK$(RESET)" || echo "$(YELLOW)⚠️  API: indisponível$(RESET)"
	@curl -sf http://localhost/health && echo "$(GREEN)✅ Frontend: OK$(RESET)" || echo "$(YELLOW)⚠️  Frontend: indisponível$(RESET)"
