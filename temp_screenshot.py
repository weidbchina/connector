from playwright.sync_api import sync_playwright
import time

# Give server time to start
time.sleep(10)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    page = browser.new_page()
    try:
        page.goto("http://localhost:8080/database-connector/sms-config", wait_until="networkidle")
        screenshot_path = "sms_config_screenshot.png"
        page.screenshot(path=screenshot_path, full_page=True)
        print(f"Screenshot saved to {screenshot_path}")
    except Exception as e:
        print(f"An error occurred: {e}")
    finally:
        browser.close()
