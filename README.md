# Pomodoro Web MVP (Monorepo)

Полноценный MVP веб-приложения из 2 модулей:
- `Контроль цели`: цели, задачи, фокус-сессии, фото-отчёты с AI-вердиктом, прогресс, дневной scheduler.
- `Мотивация`: генерация AI-изображений (ручная + авто каждые 6 часов), галерея, избранное, ежедневные цитаты.
- `AI чат`: диалог по выбранной цели с хранением истории в БД и расширенным контекстом пользователя (профиль, все цели и задачи).

Стек:
- Backend: Java 21, Spring Boot 3, Spring Security (JWT), Validation, JPA, Scheduler, OpenAPI.
- DB: PostgreSQL 16.
- Frontend: React + TypeScript + Vite.
- Локальная генерация изображений (опционально): FastAPI + Diffusers (`local-image-service`).
- Хранение файлов: локально в backend `uploads/` + путь в БД.
- Оркестрация: Docker Compose (`postgres + backend + frontend`).

## Структура

- `backend/` — Spring Boot API
- `frontend/` — React приложение
- `local-image-service/` — локальный сервис генерации изображений (Diffusers)
- `docker-compose.yml` — локальный запуск всего стека
- `.env.example` — пример переменных окружения

## Быстрый запуск

1. Скопируйте env:
```bash
cp .env.example .env
```

2. Поднимите проект:
```bash
docker compose up --build
```

3. Откройте:
- Frontend: `http://localhost:${FRONTEND_PORT:-5173}`
- Backend API: `http://localhost:${BACKEND_PORT:-8080}`
- Swagger UI: `http://localhost:${BACKEND_PORT:-8080}/swagger-ui.html`

Если порт `8080`/`5173`/`5432` занят, измените `BACKEND_PORT`, `FRONTEND_PORT`, `POSTGRES_PORT` в `.env`.

## AI режимы (`mock`, `openai`, `local`)

Настраивается через `.env`:

- `AI_MODE=mock` (по умолчанию):
  - никаких внешних вызовов,
  - предсказуемые ответы для анализа фото/чата/генерации изображений.

- `AI_MODE=openai`:
  - реальные вызовы OpenAI API,
  - нужны `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_VISION_MODEL`, `OPENAI_IMAGE_MODEL`.

- `AI_MODE=local`:
  - чат идёт через локальный Ollama (`OLLAMA_MODEL`),
  - по умолчанию мотивационные картинки берутся из быстрого web-источника (`https://picsum.photos`) и сохраняются в `uploads/`,
  - если нужен именно локальный Diffusers, выставьте `USE_WEB_IMAGE_FEED=false` (тогда используется `local-image-service`),
  - запускать с профилем Compose и режимом `local`:
```bash
AI_MODE=local docker compose --profile local-ai up --build
```
  - либо выставить `AI_MODE=local` в `.env`,
  - первый запуск может занять много времени из-за скачивания весов моделей,
  - один раз скачайте модель Ollama:
```bash
docker compose exec ollama ollama pull llama3.2:1b
```
  - по умолчанию для `local-image-service` используется лёгкая модель `hf-internal-testing/tiny-stable-diffusion-pipe` (стабильнее для ноутбуков по памяти).
  - если у вас больше RAM и нужна картинка получше, можно в `.env` задать `LOCAL_IMAGE_MODEL_ID=segmind/tiny-sd`.
  - чтобы лента не зависала, добавлен таймаут `AI_IMAGE_TIMEOUT_SECONDS` (по умолчанию 8 сек).

## Основные API эндпоинты

Все endpoint'ы под `/api`, кроме Swagger и статики `/uploads/**`.

- Auth: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`
- Goals: `GET/POST /api/goals`, `GET/PUT/DELETE /api/goals/{id}`
- Tasks: `GET/POST /api/goals/{id}/tasks`, `PUT/DELETE /api/goals/{goalId}/tasks/{taskId}`
- Focus: `POST /api/goals/{id}/focus/start`, `POST /api/goals/{id}/focus/stop`, `GET /api/goals/{id}/focus`
- Reports: `POST /api/goals/{id}/reports` (multipart `file + comment`), `GET /api/goals/{id}/reports`
- Motivation: `POST /api/goals/{id}/motivation/generate`, `GET /api/goals/{id}/motivation`, `GET /api/goals/{id}/motivation/quote`, `PATCH /api/motivation/{imgId}/favorite`, `DELETE /api/motivation/{imgId}`
- Chat: `POST /api/goals/{id}/chat/send`, `GET /api/goals/{id}/chat/history`
- Stats: `GET /api/goals/{id}/stats`

## Схема БД

Явно задана миграциями Flyway: `V1__init.sql` + `V2__chat_context_theme_and_motivation_feed.sql`.

Базовые таблицы:
- `users`
- `goals`
- `tasks`
- `focus_sessions`
- `reports`
- `motivation_images`
- `chat_threads`
- `chat_messages`
- `refresh_tokens`
- `daily_summaries`
- `motivation_quotes`

Ключевые дополнения V2:
- `users.full_name` — имя пользователя для контекста AI-чата.
- `goals.theme_color` — цвет цели (используется для фона UI при переключении целей).
- `motivation_images.generated_by`, `motivation_images.favorited_at` — источник картинки и логика пина избранного на 24 часа.

## Scheduler

`DailyScheduler` запускается ежедневно в `00:05` (временная зона контейнера backend):
- переводит `PENDING` отчёты прошлых дней в `OVERDUE`,
- пересчитывает `streak`,
- формирует `daily_summaries`.

Дополнительные cron-задачи:
- каждые 6 часов: авто-генерация мотивационных изображений для всех целей.
- ежедневно в `00:10`: генерация/обновление ежедневной цитаты для каждой цели.

## JWT и безопасность

- JWT `access + refresh`.
- Роли: `USER`, `ADMIN`.
- API закрыт авторизацией, кроме `/api/auth/*`.
- Единый JSON-формат ошибок (`ErrorResponse`) + глобальный handler.

## Команды качества и тесты

### Backend

Локально (через Maven Wrapper):
```bash
cd backend
./mvnw spotless:check test
```

Через Docker (без локального Maven):
```bash
docker run --rm -v "$PWD/backend":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -q spotless:check test
```

Покрытие: 10 integration тестов (auth, security, goals/tasks, focus, reports, motivation, chat, scheduler).

### Frontend

Через Docker (без локального Node):
```bash
docker run --rm -v "$PWD/frontend":/app -w /app node:22-alpine sh -lc 'npm install && npm run lint && npm run test:run && npm run build'
```

Покрытие: 4 тестовых файла / 7 тестов.

## Что можно сделать сразу после `docker compose up`

1. Зарегистрироваться / войти.
2. Создать цель (включая цвет) и задачи.
3. Запустить/остановить фокус-сессию.
4. Загрузить фото-отчёт и получить AI verdict (`mock` или `openai`).
5. Получать авто-картинки (каждые 6 часов) и добавлять в избранное (пин в ленте 24 часа).
6. Видеть ежедневную цитату с автором.
7. Отправить сообщение в чат и получить ответ с учетом всех целей/задач пользователя.

## Принятые решения и допущения

- Для прозрачности схемы включён Flyway, `ddl-auto=validate`.
- Статистика в экране `Статистика` строится из `daily_summaries` (после запуска scheduler).
- Изображения доступны по URL `/uploads/...` для удобного рендера в frontend.
- Для регистрации frontend запрашивает имя (`fullName`), но backend также умеет подставить имя из email, если поле не передано.
- В `mock` режиме мотивационные изображения генерируются как SVG-заглушки с контекстом цели.
- В `local` режиме анализ фото работает по эвристике комментария (локальная vision-модель не подключалась).
