# Motivation feed photos fix verification

Date: 2026-05-10

## Что исправлено

- Backend больше не считает старые SVG-заглушки из `/uploads/*.svg` валидными мотивационными изображениями.
- При недоступности Wikimedia backend создаёт fallback-карточки с реальными фото-ссылками, а не локальными SVG-заглушками.
- Frontend больше не показывает текстовую SVG-заглушку при ошибке загрузки изображения: вместо неё подставляется фото fallback через `https://picsum.photos/seed/...`.
- Тест мотивационной ленты обновлён под новое поведение: при ошибке изображения ожидается фото fallback, а не `data:image/svg+xml`.

## Проверки

### Backend tests

Command:

```bash
docker run --rm -v "$PWD":/app -w /app maven:3.9.9-eclipse-temurin-21 mvn test
```

Result:

```text
Tests run: 45, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Во время тестов Wikimedia несколько раз вернула `429 Too Many Requests`; это ожидаемо для внешнего источника. Fallback-логика отработала без падения.

### Frontend tests and build

Command:

```bash
docker run --rm -v "$PWD":/app -w /app node:22-alpine sh -lc "npm install && npm test -- --run && npm run build"
```

Result:

```text
Test Files 7 passed (7)
Tests 19 passed (19)
✓ built in 1.00s
```

NPM audit warning remains existing:

```text
8 vulnerabilities (6 moderate, 2 high)
```

### Docker Compose rebuild

Command:

```bash
docker compose up -d --build && docker compose ps
```

Result:

```text
playground-backend   Built
playground-frontend  Built
playground-backend-1 Up 0.0.0.0:18080->8080/tcp
playground-frontend-1 Up 0.0.0.0:5173->5173/tcp
playground-postgres-1 Up 0.0.0.0:5432->5432/tcp
```

## Manual expectation

Open `http://localhost:5173/motivation`, refresh the page or click `Обновить ленту`.
If the original internet image fails, the card should now show a real fallback photo instead of a pale text/SVG placeholder.
