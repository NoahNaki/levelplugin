const fs = require("fs");
const path = require("path");
const { spawn } = require("child_process");
const { chromium, request } = require("playwright");

const SERVER_ID = process.env.DATHOST_SERVER_ID || "695bf2ba5c7cf5b7f51be191";

const CHROME_DEBUG_URL = "http://127.0.0.1:9222";
const CHROME_EXE = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
const CHROME_PROFILE = "C:\\Users\\noahs\\chrome-debug-profile";

const COOKIE_FILE = path.join(__dirname, "dathost-session.txt");

function readSavedCookie() {
  if (process.env.DATHOST_SESSION && process.env.DATHOST_SESSION.trim()) {
    return process.env.DATHOST_SESSION.trim();
  }

  if (fs.existsSync(COOKIE_FILE)) {
    return fs.readFileSync(COOKIE_FILE, "utf8").trim();
  }

  return null;
}

function saveCookie(cookie) {
  fs.writeFileSync(COOKIE_FILE, cookie, "utf8");
}

async function rebootWithCookie(cookie) {
  const context = await request.newContext({
    baseURL: "https://dathost.net",
    extraHTTPHeaders: {
      accept: "application/json, text/*",
      referer: "https://dathost.net/control-panel/game-servers",
      "x-no-www-authenticate": "true",
      cookie,
    },
  });

  const response = await context.post(`/api/0.1/game-servers/${SERVER_ID}/start`);
  const ok = response.ok();

  if (!ok) {
    const body = await response.text().catch(() => "");
    console.log(`DatHost API failed: ${response.status()} ${response.statusText()}`);
    if (body) console.log(body);
  }

  await context.dispose();
  return ok;
}

async function waitForDebugChrome(timeoutMs = 15000) {
  const started = Date.now();

  while (Date.now() - started < timeoutMs) {
    try {
      const response = await fetch(`${CHROME_DEBUG_URL}/json/version`);
      if (response.ok) return true;
    } catch (_) {}

    await new Promise((resolve) => setTimeout(resolve, 500));
  }

  return false;
}

function openDebugChrome() {
  console.log("Opening debug Chrome...");

  spawn(
    CHROME_EXE,
    [
      "--remote-debugging-port=9222",
      `--user-data-dir=${CHROME_PROFILE}`,
      "https://dathost.net/control-panel/game-servers",
    ],
    {
      detached: true,
      stdio: "ignore",
    }
  ).unref();
}

async function getSessionCookieFromChrome() {
  const browser = await chromium.connectOverCDP(CHROME_DEBUG_URL);
  const context = browser.contexts()[0];

  if (!context) {
    throw new Error("No Chrome context found.");
  }

  const page = await context.newPage();

  await page.goto("https://dathost.net/control-panel/game-servers", {
    waitUntil: "domcontentloaded",
  });

  console.log("Log into DatHost in the opened Chrome window.");
  console.log("Make sure you can see the DatHost game servers page.");
  console.log("After you are logged in, press ENTER here...");

  await new Promise((resolve) => process.stdin.once("data", resolve));

  await page.goto("https://dathost.net/control-panel/game-servers", {
    waitUntil: "networkidle",
  });

  const cookies = await context.cookies();

  const dathostCookies = cookies.filter((cookie) =>
    cookie.domain.includes("dathost")
  );

  console.log("DatHost cookies found:");
  for (const cookie of dathostCookies) {
    console.log(`${cookie.name} | ${cookie.domain} | ${cookie.path}`);
  }

  const sessionCookie = dathostCookies.find(
    (cookie) => cookie.name === "session"
  );

  await page.close();
  await browser.close();

  if (!sessionCookie?.value) {
    throw new Error("Could not find DatHost session cookie after login.");
  }

  const cookieHeader = `session=${sessionCookie.value}`;

  console.log(`Using DatHost session cookie from ${sessionCookie.domain}`);
  saveCookie(cookieHeader);

  return cookieHeader;
}

async function main() {
  const savedCookie = readSavedCookie();

  if (savedCookie) {
    console.log("Trying saved DatHost session cookie...");

    if (await rebootWithCookie(savedCookie)) {
      console.log("DatHost reboot request sent using saved cookie.");
      return;
    }

    console.log("Saved DatHost cookie is invalid or expired.");
  } else {
    console.log("No saved DatHost session cookie found.");
  }

  openDebugChrome();

  const chromeReady = await waitForDebugChrome();

  if (!chromeReady) {
    throw new Error("Debug Chrome did not start or port 9222 is unavailable.");
  }

  const freshCookie = await getSessionCookieFromChrome();

  console.log("Retrying DatHost reboot with fresh cookie...");

  if (!(await rebootWithCookie(freshCookie))) {
    throw new Error("DatHost reboot failed even after fresh login.");
  }

  console.log("DatHost reboot request sent using fresh cookie.");
}

main().catch((err) => {
  console.error(err.message);
  process.exit(1);
});