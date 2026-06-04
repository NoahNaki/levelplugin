# Local Deploy Workflow

This project is built with Maven and produces:

```text
target/levelplugin.jar
```

The fastest development loop is:

```text
Codex updates branch
→ pull/checkout branch locally
→ run scripts\deploy-dev.bat
→ script builds, uploads the JAR and restarts the server
→ test in game
```

## Why this helps

Previously, the loop was manual:

```text
build artifact
→ find JAR
→ open FileZilla
→ drag JAR to plugins folder
→ restart server manually
```

The deployment script replaces that with one command.

## Requirements

The recommended setup is **SFTP/SSH**, not plain FTP.

You need:

- SSH/SFTP access to the remote server.
- The remote path to the Minecraft `plugins` folder.
- A restart command that works on the remote host.

FileZilla usually uses one of these protocols:

- **SFTP**: good; the script can use `scp`/`ssh`.
- **FTP/FTPS only**: upload can be automated with tools like WinSCP, but restart still requires SSH, panel API or RCON.

Uploading a JAR and restarting are two different operations. File transfer alone cannot restart the Minecraft server.

## Setup

1. Copy:

```text
scripts\deploy-config.example.ps1
```

to:

```text
scripts\deploy-config.ps1
```

2. Fill in:

```powershell
$ServerHost = "example.com"
$ServerPort = 22
$ServerUser = "minecraft"
$RemotePluginsDir = "/home/minecraft/server/plugins"
$RestartCommand = "sudo systemctl restart minecraft"
```

3. Make sure `scripts/deploy-config.ps1` is not committed. It is ignored by `.gitignore`.

## Usage

From the repository root:

```bat
scripts\deploy-dev.bat
```

Or directly:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\deploy-dev.ps1
```

Skip the Maven build if the JAR already exists:

```bat
scripts\deploy-dev.bat -SkipBuild
```

Upload without restarting:

```bat
scripts\deploy-dev.bat -NoRestart
```

## Restart command examples

### systemd

```powershell
$RestartCommand = "sudo systemctl restart minecraft"
```

### docker compose

```powershell
$RestartCommand = "cd /home/minecraft/server && docker compose restart"
```

### screen

```powershell
$RestartCommand = "screen -S minecraft -p 0 -X stuff 'restart^M'"
```

### tmux

```powershell
$RestartCommand = "tmux send-keys -t minecraft 'restart' Enter"
```

## If your host has only FTP/FileZilla access

Use WinSCP CLI for upload automation, but you still need one of these for restart:

- SSH command.
- Hosting panel API.
- RCON with a configured restart command.
- Manual restart in the host panel.

Plain FTP cannot restart a process.
