const { Rcon } = require("rcon-client");

async function main() {
  const host = process.env.RCON_HOST;
  const port = Number(process.env.RCON_PORT);
  const password = process.env.RCON_PASS;
  const command = process.argv.slice(2).join(" ");

  if (!host || !port || !password || !command) {
    console.error("Missing RCON_HOST, RCON_PORT, RCON_PASS, or command.");
    process.exit(1);
  }

  const rcon = await Rcon.connect({
    host,
    port,
    password,
  });

  const response = await rcon.send(command);
  console.log(response);

  await rcon.end();
}

main().catch((err) => {
  console.error("RCON failed:", err.message);
  process.exit(1);
});