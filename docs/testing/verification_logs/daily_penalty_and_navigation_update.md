# Проверка: ежедневные штрафы и правки навигации

Дата проверки: 2026-05-08

## Что проверялось

- Inline-предупреждение о похожей активной цели внутри карточки `Создать цель`.
- Переименование пунктов меню: `Dashboard` -> `Обзор`, `Контроль` -> `Цель`.
- Автоматическое ежедневное списание виртуальных монет за дни без принятого отчёта.
- Перерасчёт уже существующих активных целей с даты создания до вчерашнего дня.
- Идемпотентность списаний через `wallet_transactions(goal_id, penalty_date)`.
- История цели для списаний и дней, где штраф не применён.
- Docker Compose запуск после миграции `V7__daily_report_penalty_dates.sql`.

## Timezone

В проект добавлен явный timezone для Docker:

- `.env.example`: `APP_TIME_ZONE=Europe/Moscow`
- `docker-compose.yml`: backend получает `TZ=${APP_TIME_ZONE:-Europe/Moscow}`

Scheduler использует единый timezone приложения через `ZoneId.systemDefault()`. В Docker после перезапуска проверено:

```bash
docker compose exec backend date
# Fri May  8 01:49:30 PM MSK 2026
```

## Команды

### Backend targeted test

```bash
docker run --rm -v "$PWD/backend":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -q -Dtest=GoalExperienceIntegrationTest test
```

Результат: успешно.

### Backend full test suite

```bash
docker run --rm -v "$PWD/backend":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -q test
```

Результат: успешно.

Примечание: во время мотивационных тестов Wikimedia Commons вернул `429 Too Many Requests`; fallback-логика мотивационной ленты отработала, тесты прошли.

### Frontend tests

```bash
docker run --rm -v "$PWD/frontend":/app -w /app node:22-alpine sh -lc 'npm install >/tmp/npm-install.log && npm run test:run -- ControlPage ProfilePage'
```

Результат: успешно.

```text
Test Files  2 passed (2)
Tests       4 passed (4)
```

### Frontend build

```bash
docker run --rm -v "$PWD/frontend":/app -w /app node:22-alpine sh -lc 'npm install >/tmp/npm-install.log && npm run build'
```

Результат: успешно.

```text
vite v5.4.21 building for production...
✓ built in 837ms
```

### Docker Compose

```bash
docker compose up -d --build
docker compose ps
```

Результат: успешно.

```text
playground-backend-1    Up    0.0.0.0:18080->8080/tcp
playground-frontend-1   Up    0.0.0.0:5173->5173/tcp
playground-postgres-1   Up    0.0.0.0:5432->5432/tcp
```

Flyway применил новую миграцию:

```text
Migrating schema "public" to version "7 - daily report penalty dates"
Successfully applied 1 migration to schema "public", now at version v7
```

## Итог

- Backend тесты прошли.
- Frontend тесты прошли.
- Frontend production build прошёл.
- Docker Compose поднялся.
- Backend стартовал и применил миграцию V7.
- Timezone backend-контейнера установлен в MSK.
