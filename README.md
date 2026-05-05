# Pomodoro Web MVP (Monorepo)

Pomodoro Web теперь описывается как единый `Goal Experience`, а не как набор разрозненных режимов.

Основная идея продукта:
- пользователь создаёт цель;
- настраивает ежедневное обязательство;
- выполняет Pomodoro-сессии;
- отправляет фото-отчёт;
- AI подтверждает или отклоняет прогресс;
- система пересчитывает streak, discipline score, риск и прогноз достижения цели;
- мотивация, изображения, цитаты и рекомендации подстраиваются под реальное состояние цели;
- профиль пользователя хранит историю выполненных и невыполненных целей, аватар и личную статистику;
- активные цели защищены от точных и смысловых дублей.

Product flow:

`goal → commitment → Pomodoro → report → AI verification → streak / discipline score / risk / forecast → motivation`

## Стек

- Backend: Java 21, Spring Boot 3, Spring Security (JWT), Validation, JPA, Scheduler, OpenAPI
- DB: PostgreSQL 16
- Frontend: React + TypeScript + Vite
- Источник мотивационных изображений: Wikimedia Commons API
- Хранение файлов: локально в backend `uploads/` + путь в БД
- Оркестрация: Docker Compose (`postgres + backend + frontend`)

## Структура

- `backend/` — Spring Boot API
- `frontend/` — React приложение
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
- Swagger UI: `http://localhost:${BACKEND_PORT:-8080}/swagger-ui/index.html`

Если порт `8080`/`5173`/`5432` занят, измените `BACKEND_PORT`, `FRONTEND_PORT`, `POSTGRES_PORT` в `.env`.

## AI режимы (`mock`, `openai`, `local`)

Настраивается через `.env`:

- `AI_MODE=mock`
  - никаких внешних вызовов,
  - предсказуемые ответы для анализа фото, рекомендаций и чата.

- `AI_MODE=openai`
  - реальные вызовы OpenAI API,
  - нужны `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_VISION_MODEL`.

- `AI_MODE=local`
  - чат идёт через локальный Ollama (`OLLAMA_MODEL`),
  - мотивационные картинки для ленты подбираются из интернета по теме цели,
  - изображения сохраняются в `uploads/` и становятся частью истории цели.

## Что изменилось в продукте

### Вертикальная лента мотивации

Вкладка `Мотивация` больше не использует grid/columns layout. Вместо этого:
- карточки показываются вертикальной лентой, по одной крупной карточке за экран;
- контейнер использует scroll-snap по оси `y`, поэтому лента ощущается как Reels / Shorts;
- кнопка `Обновить ленту` перезагружает feed и возвращает пользователя к первой карточке;
- если активная цель не выбрана, показывается понятный empty-state;
- если изображения для цели временно не найдены, показывается отдельный empty-state без технического мусора.

### Как решена проблема битых картинок и мусорных заголовков

Backend и frontend вместе фильтруют некачественные источники:
- backend не отдаёт feed-карточки с `pdf/html/file` ссылками вместо изображения;
- backend санитизирует технические titles/descriptions и формирует пользовательские `caption` и `goalReason`;
- frontend не показывает raw URL, file names и мусорные строки;
- если картинка не загрузилась, карточка переключается на безопасный SVG fallback вместо broken image / alt-файла.

### Feedback по изображениям

Для каждой карточки доступны действия:
- `Неинтересно`
- `Пожаловаться`

Логика работает серверно и хранится в PostgreSQL:
- `Неинтересно` скрывает карточку только для этого пользователя;
- `Пожаловаться` скрывает карточку для пользователя и сохраняет причину;
- после 3 жалоб от разных пользователей карточка скрывается глобально;
- повторный feedback идемпотентен и не создаёт дублей.

### Защита от дублей целей

Нельзя создать:
- точный дубль активной цели;
- смыслово похожую активную цель.

Сервис нормализует текст, удаляет шум, сравнивает ключевые токены и блокирует похожие активные цели.
Если старая цель уже завершена или закрыта как невыполненная, похожую новую цель создать можно.

### Профиль и история целей

Появилась отдельная вкладка `Профиль`, где есть:
- email и имя пользователя;
- загрузка аватара;
- статистика по фокус-сессиям, streak и дисциплине;
- список активных целей;
- история завершённых и невыполненных целей.

Если цель закрывается без завершения:
- она не удаляется бесследно;
- переводится в `FAILED`;
- сохраняется причина;
- остаётся в истории профиля;
- помечается отдельным badge как невыполненная цель.

### Улучшенный мотиватор

Мотиватор теперь получает расширенный контекст:
- активную цель и все текущие цели;
- дневную норму и прогресс за сегодня;
- streak, discipline score и risk status;
- последние фокус-сессии;
- последние отчёты;
- последние события цели;
- историю чата;
- feedback по мотивационным карточкам;
- профиль пользователя и историю завершённых/проваленных целей.

За счёт этого он лучше отвечает на вопросы:
- что делать сегодня;
- почему пользователь отстаёт;
- как не сорвать streak;
- как спланировать вечер;
- как мягко восстановить темп без перегруза.

## Goal Experience

Каждая цель теперь выступает центром продукта и объединяет:
- описание цели и дедлайн;
- ежедневное обязательство (`goal_commitments`);
- задачи на день;
- Pomodoro / focus sessions;
- фото-отчёты с AI-проверкой;
- streak и discipline score;
- статус риска (`LOW`, `MEDIUM`, `HIGH`);
- прогноз достижения цели;
- личную награду;
- мотивационные изображения и цитаты;
- AI / rule-based recommendation;
- таймлайн событий цели (`goal_events`).

### Новые сущности

- `goal_commitments`
  - ежедневная норма в минутах;
  - период действия;
  - completedDays / missedDays;
  - disciplineScore;
  - currentStreak / bestStreak;
  - riskStatus;
  - personalRewardTitle / personalRewardDescription;
  - rewardUnlocked.

- `goal_events`
  - история создания цели, фокус-сессий, отчётов, изменения streak, discipline score, риска и награды.

Также у `goals` появился lifecycle:
- `ACTIVE`
- `COMPLETED`
- `FAILED`
- `ARCHIVED`

- `motivation_image_feedbacks`
  - хранит персональные сигналы пользователя по изображениям;
  - тип feedback: `NOT_INTERESTED` или `REPORTED`;
  - жалобы учитываются глобально, а `Неинтересно` работает персонально.

- `reports.ai_confidence`
  - сохраняет confidence из AI-анализа фото-отчёта.

- `users.avatar_path`
  - хранит путь к пользовательскому аватару.

## Схема БД

Схема задаётся Flyway-миграциями:
- `V1__init.sql`
- `V2__chat_context_theme_and_motivation_feed.sql`
- `V3__goal_commitment_and_goal_events.sql`
- `V4__motivation_image_feedback.sql`
- `V5__goal_lifecycle_and_profile.sql`

Ключевые таблицы:
- `users`
- `goals`
- `tasks`
- `focus_sessions`
- `reports`
- `daily_summaries`
- `goal_commitments`
- `goal_events`
- `motivation_images`
- `motivation_image_feedbacks`
- `motivation_quotes`
- `chat_threads`
- `chat_messages`
- `refresh_tokens`

## Основные API endpoints

Все endpoints находятся под `/api`, кроме Swagger и статики `/uploads/**`.

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`

### Goals
- `GET /api/goals`
- `POST /api/goals`
- `GET /api/goals/{id}`
- `PUT /api/goals/{id}`
- `DELETE /api/goals/{id}`
- `POST /api/goals/{id}/close-failed`

### Tasks
- `GET /api/goals/{id}/tasks`
- `POST /api/goals/{id}/tasks`
- `PUT /api/goals/{goalId}/tasks/{taskId}`
- `DELETE /api/goals/{goalId}/tasks/{taskId}`

### Focus
- `POST /api/goals/{id}/focus/start`
- `POST /api/goals/{id}/focus/stop`
- `GET /api/goals/{id}/focus`

### Reports
- `POST /api/goals/{id}/reports`
- `GET /api/goals/{id}/reports`

### Goal Experience
- `POST /api/goals/{goalId}/commitment`
- `GET /api/goals/{goalId}/commitment`
- `GET /api/goals/{goalId}/today`
- `GET /api/goals/{goalId}/forecast`
- `GET /api/goals/{goalId}/events`
- `GET /api/goals/{goalId}/experience`
- `GET /api/goal-experience`

### Motivation
- `POST /api/goals/{id}/motivation/generate`
- `GET /api/goals/{id}/motivation`
- `GET /api/goals/{id}/motivation/quote`
- `POST /api/goals/{id}/motivation/refresh-feed`
- `GET /api/motivation/feed?goalId={goalId}&limit=10`
- `POST /api/motivation/images/{imageId}/not-interested`
- `POST /api/motivation/images/{imageId}/report`
- `PATCH /api/motivation/{imgId}/favorite`
- `DELETE /api/motivation/{imgId}`

### Chat / Motivator
- `POST /api/goals/{id}/chat/send`
- `GET /api/goals/{id}/chat/history`
- `DELETE /api/goals/{id}/chat/history`

### Stats
- `GET /api/goals/{id}/stats`

### Profile
- `GET /api/profile`
- `PUT /api/profile`
- `POST /api/profile/avatar`
- `GET /api/profile/goals`

## Scheduler

`DailyScheduler` запускается ежедневно в `00:05` и делает реальные nightly-перерасчёты:
- переводит старые `PENDING` отчёты в `OVERDUE`;
- создаёт `daily_summaries`;
- вызывает processing прошлого дня для `goal_commitments`.

Логика закрытия дня строится на реальных данных прошлого дня:
- focus minutes по цели;
- наличие подтверждённого фото-отчёта;
- изменение streak;
- изменение discipline score;
- перерасчёт riskStatus;
- разблокировка награды при успешном завершении обязательства.

Никаких simulate day / fake flow в проекте нет.

## Мотивационный блок

Мотивационная лента теперь работает как персонализированный feed, а не как статическая галерея:
- backend подбирает до 10 изображений из интернета по теме выбранной цели;
- frontend показывает их вертикальной лентой с большим изображением, подписью, цитатой и объяснением связи с целью;
- изображения сохраняются в PostgreSQL (`motivation_images`) и переиспользуются между пользователями;
- пользователь может отметить изображение как `Неинтересно`;
- пользователь может пожаловаться на неподходящее изображение;
- feedback хранится в PostgreSQL (`motivation_image_feedbacks`);
- изображения, скрытые пользователем, больше не возвращаются именно этому пользователю;
- изображения, набравшие 3 и более жалобы от разных пользователей, скрываются глобально (`hidden_globally = true`).

Это сделано серверно: фронтенд только отправляет действие, а фильтрация выдачи выполняется на backend.

## AI-проверка отчётов

AI-анализ фото-отчётов стал строже и оценивает именно доказательство выполнения задачи:
- AI получает цель, описание цели, задачи на сегодня, комментарий пользователя и критерии принятия;
- просмотр видео засчитывается только если задача действительно про лекцию / видео;
- для практических задач нужны признаки результата: код, IDE, конспект, документ, упражнение, тесты, проектный артефакт;
- если доказательств недостаточно, отчет получает `NEEDS_MORE_INFO`, а статус отчёта остаётся `PENDING`;
- если фото не связано с целью и задачами, AI возвращает `REJECTED`;
- при ошибке AI backend не подтверждает отчёт автоматически, а безопасно возвращает `NEEDS_MORE_INFO`.

Пример product-логики:
- задача `Посмотреть лекцию по алгоритмам` -> скрин с видео может быть допустим;
- задача `Написать код сервиса и запустить тесты` -> одного YouTube-видео недостаточно, нужен результат.

## JWT и безопасность

- JWT `access + refresh`
- роли: `USER`, `ADMIN`
- все endpoints закрыты, кроме `/api/auth/*`
- единый JSON-формат ошибок + global exception handler
- чужой пользователь не может читать или изменять `goal experience` чужой цели
- profile endpoints и feedback по мотивационным карточкам доступны только авторизованному пользователю

## Команды качества и тесты

### Backend

Локально:
```bash
cd backend
./mvnw spotless:check test
```

Если локальный JDK конфликтует с Lombok, можно использовать контейнер с Java 21:
```bash
docker run --rm -v "$PWD/backend":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn -q spotless:check test
```

### Frontend

```bash
cd frontend
npm install
npm run lint
npm run test:run
npm run build
```

Или без локального Node:
```bash
docker run --rm -v "$PWD/frontend":/app -w /app node:22-alpine sh -lc 'npm install && npm run lint && npm run test:run && npm run build'
```

### Docker Compose

Полная пересборка и запуск:
```bash
docker compose up --build
```

Для фонового запуска:
```bash
docker compose up --build -d
```

## Демонстрация проекта

Для дипломной демонстрации важно использовать реальный накопленный аккаунт, а не demo-flow. В аккаунте должны быть:
- цели;
- ежедневные обязательства;
- задачи по целям;
- фокус-сессии;
- фото-отчёты;
- feedback по мотивационным изображениям;
- daily summaries;
- события цели (`goal_events`);
- статистика и мотивационная лента.

Тогда система показывает цель как единый сценарий: что нужно сделать сегодня, какой риск, какой прогноз, что уже подтверждено AI и какая следующая рекомендация.

## Принятые решения

- backend не использует `ddl-auto update`, а опирается на Flyway;
- день считается выполненным, если достигнута дневная норма по фокусу и есть `CONFIRMED` фото-отчёт;
- riskStatus считается по простой и объяснимой логике:
  - `LOW`: disciplineScore `>= 75` и `missedDays <= 1`
  - `MEDIUM`: disciplineScore `50-74` или `missedDays 2-3`
  - `HIGH`: disciplineScore `< 50` или `missedDays >= 4`
- прогноз строится на `targetHours`, `focus_sessions`, `daily_summaries` и текущем discipline score;
- AI recommendation на первом этапе rule-based, чтобы не ломать `mock/local/openai` режимы.
- одинаковые и очень похожие активные цели запрещены серверной логикой;
- удаление активной цели заменено на явное закрытие как невыполненной с обязательной причиной;
- аватары хранятся в `uploads/avatars`, поддерживаются `jpg/png/webp`, размер ограничен 3 МБ.
