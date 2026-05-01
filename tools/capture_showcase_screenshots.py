from pathlib import Path
import time

from selenium import webdriver
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

BASE_URL = 'http://localhost:5173'
ACCESS_TOKEN = 'eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIyOCIsInR5cGUiOiJhY2Nlc3MiLCJyb2xlIjoiVVNFUiIsImp0aSI6Ijc3N2M5MzM1LTUzMWEtNDQ1ZC04ODE1LTRkMDVmOWE0Yzc2MiIsImlhdCI6MTc3MzY3ODY2MiwiZXhwIjoxNzczNjgwNDYyfQ.lOgpprNbLuYs8cKeEmn_GbSSJiKQM0O6S4D-8_paqYlvFdtsOgh5ip3lCq6YkzNr'
REFRESH_TOKEN = 'eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiIyOCIsInR5cGUiOiJyZWZyZXNoIiwianRpIjoiNDRiZTc3NjAtZjQxMy00MDU3LTgzYTAtYWJkMDQ4ODZlZDkwIiwiaWF0IjoxNzczNjc4NjYyLCJleHAiOjE3NzQyODM0NjJ9.29pRAP1OcIonwI8QjqVZpdr6RNueh4MXxVIcL_qB4GSV1CICdLJGoy3w5wdagrca'
GOAL_ID = '26'
OUT_DIR = Path('/Users/kamilhus/Documents/Playground/tmp/showcase_screenshots')
OUT_DIR.mkdir(parents=True, exist_ok=True)


options = Options()
options.add_argument('--headless=new')
options.add_argument('--disable-gpu')
options.add_argument('--window-size=1600,1800')
options.add_argument('--lang=ru-RU')
options.add_argument('--force-device-scale-factor=1')
options.add_argument('--hide-scrollbars')
options.add_argument('--no-sandbox')
options.add_experimental_option('excludeSwitches', ['enable-automation'])
options.add_experimental_option('prefs', {'intl.accept_languages': 'ru-RU,ru'})


def wait_text(driver, text):
    WebDriverWait(driver, 25).until(EC.presence_of_element_located((By.TAG_NAME, 'body')))
    WebDriverWait(driver, 25).until(lambda d: text in d.find_element(By.TAG_NAME, 'body').text)


def stabilize(driver):
    time.sleep(1.2)
    driver.execute_script("window.scrollTo(0, 0);")
    driver.execute_script("document.body.style.zoom='0.9';")
    time.sleep(0.4)


def save(driver, name):
    path = OUT_DIR / name
    driver.save_screenshot(str(path))
    print(path)


driver = webdriver.Chrome(options=options)
try:
    driver.get(f'{BASE_URL}/login')
    wait_text(driver, 'Войти')
    stabilize(driver)
    save(driver, '01-login.png')

    driver.execute_script(
        "localStorage.setItem('pomodoro_access_token', arguments[0]);"
        "localStorage.setItem('pomodoro_refresh_token', arguments[1]);"
        "localStorage.setItem('pomodoro_selected_goal_id', arguments[2]);"
        "localStorage.setItem('pomodoro_nav_collapsed', '0');",
        ACCESS_TOKEN,
        REFRESH_TOKEN,
        GOAL_ID,
    )

    routes = [
        ('02-dashboard.png', '/', 'Последние отчеты'),
        ('03-control.png', '/control', 'Редактирование цели'),
        ('04-focus.png', '/focus', 'Проверка AI (фото-отчет)'),
        ('05-motivation.png', '/motivation', 'Лента мотивации'),
        ('06-chat.png', '/chat', 'Мотиватор'),
        ('07-stats.png', '/stats', 'Статистика за последние 14 дней'),
    ]

    for filename, route, text in routes:
        driver.get(f'{BASE_URL}{route}')
        wait_text(driver, text)
        stabilize(driver)
        if route == '/motivation':
            try:
                WebDriverWait(driver, 10).until(EC.presence_of_element_located((By.CSS_SELECTOR, '.motivation-post-image')))
            except TimeoutException:
                pass
        save(driver, filename)
finally:
    driver.quit()
