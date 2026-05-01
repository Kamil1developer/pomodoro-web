import json
import shutil
import subprocess
import time
import urllib.error
import urllib.request
from pathlib import Path

BASE = 'http://localhost:18080/api'
EMAIL = 'report-showcase@example.com'
PASSWORD = 'Password123!'
FULL_NAME = 'Хусаинов Камиль Фанисович'
ASSET_DIR = Path('/Users/kamilhus/Documents/Playground/tmp/showcase_assets')
ASSET_DIR.mkdir(parents=True, exist_ok=True)

IMAGE_SOURCES = {
    'report.jpg': Path('/Users/kamilhus/Downloads/Time Management Illustration.png'),
    'motivation-1.jpg': Path('/Users/kamilhus/Downloads/Capture-2026-02-05-032014.png'),
    'motivation-2.jpg': Path('/Users/kamilhus/Downloads/Time Management Illustration.png'),
    'motivation-3.jpg': Path('/Users/kamilhus/Downloads/road.jpg'),
}

for target_name, source in IMAGE_SOURCES.items():
    if not source.exists():
        raise SystemExit(f'Asset not found: {source}')
    shutil.copy(source, ASSET_DIR / target_name)


def req(path, method='GET', token=None, body=None):
    headers = {}
    data = None
    if token:
        headers['Authorization'] = f'Bearer {token}'
    if body is not None:
        data = json.dumps(body).encode('utf-8')
        headers['Content-Type'] = 'application/json'
    request = urllib.request.Request(BASE + path, method=method, headers=headers, data=data)
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            raw = response.read()
            if not raw:
                return None
            return json.loads(raw)
    except urllib.error.HTTPError as exc:
        payload = exc.read().decode('utf-8', errors='ignore')
        raise RuntimeError(f'{method} {path} failed: {exc.code} {payload}') from exc


def auth_tokens():
    try:
        data = req('/auth/register', method='POST', body={
            'email': EMAIL,
            'password': PASSWORD,
            'fullName': FULL_NAME,
        })
    except RuntimeError as exc:
        if '400' not in str(exc) and '409' not in str(exc):
            raise
        data = req('/auth/login', method='POST', body={'email': EMAIL, 'password': PASSWORD})
    return data['accessToken'], data['refreshToken']


def psql(sql: str) -> str:
    cmd = [
        'docker', 'exec', '-i', 'playground-postgres-1',
        'psql', '-X', '-q', '-U', 'pomodoro', '-d', 'pomodoro', '-t', '-A', '-c', sql,
    ]
    output = subprocess.check_output(cmd, text=True).strip()
    if not output:
        return ''
    return output.splitlines()[0].strip()


def docker_cp(src: Path, container_path: str) -> None:
    subprocess.check_call(['docker', 'cp', str(src), f'playground-backend-1:{container_path}'])


def ensure_user_and_goal(token: str):
    goals = req('/goals', token=token) or []
    for goal in goals:
        if goal['title'] == 'Подготовка диплома':
            return goal['id']
    goal = req('/goals', method='POST', token=token, body={
        'title': 'Подготовка диплома',
        'description': 'Собрать Pomodoro Web, подготовить презентацию и оформить отчет по практике.',
        'targetHours': 120,
        'deadline': '2026-06-20',
        'themeColor': '#7db7f4',
    })
    return goal['id']


def ensure_tasks(token: str, goal_id: int):
    tasks = req(f'/goals/{goal_id}/tasks', token=token) or []
    titles = {task['title']: task for task in tasks}
    desired = [
        'Сделать 15-минутный разбор архитектуры проекта',
        'Подготовить структуру презентации и отчет',
        'Проверить AI-модуль и сделать демонстрационные скриншоты',
    ]
    for title in desired:
        if title not in titles:
            req(f'/goals/{goal_id}/tasks', method='POST', token=token, body={'title': title})
    tasks = req(f'/goals/{goal_id}/tasks', token=token) or []
    for index, task in enumerate(tasks[:3]):
        req(
            f'/goals/{goal_id}/tasks/{task["id"]}',
            method='PUT',
            token=token,
            body={'title': task['title'], 'isDone': index < 2},
        )


def seed_database(goal_id: int):
    user_id = psql(f"SELECT id FROM users WHERE email = '{EMAIL}';")
    if not user_id:
        raise SystemExit('User not found in DB')

    psql(f"DELETE FROM chat_messages WHERE thread_id IN (SELECT id FROM chat_threads WHERE goal_id = {goal_id});")
    psql(f"DELETE FROM chat_threads WHERE goal_id = {goal_id};")
    psql(f"DELETE FROM reports WHERE goal_id = {goal_id};")
    psql(f"DELETE FROM motivation_quotes WHERE goal_id = {goal_id};")
    psql(f"DELETE FROM motivation_images WHERE goal_id = {goal_id};")
    psql(f"DELETE FROM focus_sessions WHERE goal_id = {goal_id};")
    psql(f"DELETE FROM daily_summaries WHERE goal_id = {goal_id};")

    docker_cp(ASSET_DIR / 'report.jpg', '/app/uploads/reports/showcase-report.jpg')
    docker_cp(ASSET_DIR / 'motivation-1.jpg', '/app/uploads/motivation/showcase-1.jpg')
    docker_cp(ASSET_DIR / 'motivation-2.jpg', '/app/uploads/motivation/showcase-2.jpg')
    docker_cp(ASSET_DIR / 'motivation-3.jpg', '/app/uploads/motivation/showcase-3.jpg')

    psql(f"""
    INSERT INTO focus_sessions (goal_id, started_at, ended_at, duration_minutes) VALUES
      ({goal_id}, now() - interval '2 day' - interval '1 hour', now() - interval '2 day' - interval '35 minutes', 25),
      ({goal_id}, now() - interval '1 day' - interval '2 hour', now() - interval '1 day' - interval '1 hour 20 minutes', 40),
      ({goal_id}, now() - interval '50 minutes', now() - interval '20 minutes', 30);
    """)

    psql(f"""
    INSERT INTO reports (goal_id, report_date, comment, image_path, status, ai_verdict, ai_explanation, created_at) VALUES
      ({goal_id}, current_date - 1, 'Подготовил черновик структуры презентации и обновил интерфейс мотивации.', '/uploads/reports/showcase-report.jpg', 'CONFIRMED', 'APPROVED', 'На фото и в комментарии подтверждается работа над отчетом и интерфейсом. Отчет принят.', now() - interval '1 day'),
      ({goal_id}, current_date, 'Сделал скриншоты страниц и проверил Docker-запуск.', '/uploads/reports/showcase-report.jpg', 'PENDING', 'NEEDS_MORE_INFO', 'Комментарий понятен, но на фото не хватает явного подтверждения готовых экранов. Нужен более конкретный итоговый скриншот.', now() - interval '2 hour');
    """)

    psql(f"""
    INSERT INTO motivation_images (goal_id, image_path, prompt, is_favorite, generated_by, favorited_at, created_at) VALUES
      ({goal_id}, '/uploads/motivation/showcase-1.jpg', 'Фокус на результате и ежедневной дисциплине', false, 'AUTO', null, now() - interval '30 minute'),
      ({goal_id}, '/uploads/motivation/showcase-2.jpg', 'Управление временем и концентрацией', true, 'AUTO', now() - interval '20 minute', now() - interval '20 minute'),
      ({goal_id}, '/uploads/motivation/showcase-3.jpg', 'Движение к цели шаг за шагом', false, 'AUTO', null, now() - interval '10 minute');
    """)

    psql(f"""
    INSERT INTO motivation_quotes (goal_id, quote_text, quote_author, quote_date, created_at) VALUES
      ({goal_id}, 'Success is the sum of small efforts, repeated day in and day out.', 'Robert Collier', current_date - 2, now() - interval '2 day'),
      ({goal_id}, 'The secret of getting ahead is getting started.', 'Mark Twain', current_date - 1, now() - interval '1 day'),
      ({goal_id}, 'The future depends on what you do today.', 'Mahatma Gandhi', current_date, now());
    """)

    thread_id = psql(f"INSERT INTO chat_threads (goal_id, created_at) VALUES ({goal_id}, now() - interval '3 hour') RETURNING id;")

    psql(f"""
    INSERT INTO chat_messages (thread_id, role, content, created_at) VALUES
      ({thread_id}, 'USER', 'Помоги распределить работу по проекту на сегодня.', now() - interval '3 hour'),
      ({thread_id}, 'ASSISTANT', 'Начните с 25 минут на архитектуру, затем выделите 40 минут на отчет и отдельно 30 минут на скриншоты интерфейса. Сначала закройте один конкретный шаг, а потом переходите к следующему.', now() - interval '2 hour 59 minute'),
      ({thread_id}, 'USER', 'Что мне показать на защите в первую очередь?', now() - interval '40 minute'),
      ({thread_id}, 'ASSISTANT', 'Сначала покажите вход в систему, затем создание цели и задачи дня, потом вкладку «Фокус» с таймером и AI-проверкой, а в конце — мотивационную ленту и экран «Мотиватор». Так демонстрация будет логичной и короткой.', now() - interval '39 minute');
    """)

    psql(f"UPDATE goals SET current_streak = 5 WHERE id = {goal_id};")

    psql(f"""
    INSERT INTO daily_summaries (goal_id, summary_date, completed_tasks, focus_minutes, streak, created_at) VALUES
      ({goal_id}, current_date - 6, 1, 25, 1, now() - interval '6 day'),
      ({goal_id}, current_date - 5, 2, 40, 2, now() - interval '5 day'),
      ({goal_id}, current_date - 4, 2, 50, 3, now() - interval '4 day'),
      ({goal_id}, current_date - 3, 1, 30, 4, now() - interval '3 day'),
      ({goal_id}, current_date - 2, 3, 65, 5, now() - interval '2 day'),
      ({goal_id}, current_date - 1, 2, 40, 5, now() - interval '1 day'),
      ({goal_id}, current_date, 2, 30, 5, now());
    """)


def main():
    access_token, refresh_token = auth_tokens()
    goal_id = ensure_user_and_goal(access_token)
    ensure_tasks(access_token, goal_id)
    seed_database(goal_id)
    print(json.dumps({
        'email': EMAIL,
        'password': PASSWORD,
        'accessToken': access_token,
        'refreshToken': refresh_token,
        'goalId': goal_id,
    }, ensure_ascii=False))


if __name__ == '__main__':
    main()
