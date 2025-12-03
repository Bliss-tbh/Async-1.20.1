[![Issues](https://img.shields.io/github/issues/AxalotLDev/Async?style=for-the-badge)](https://github.com/AxalotLDev/Async/issues)
<img width="100" src="https://github.com/AxalotLDev/Async/raw/ver/1.21.1/src/main/resources/assets/async/icon.png" alt="Async icon" align="right">
<div align="left">
<h1>Async 1.20.1 & Neoforge - Minecraft Entity Multi-Threading Mod</h1>
<h3>Async is a mod designed to improve entity performance by processing them in parallel using multiple CPU cores and threads.</h3>
</div>

## Important❗

Async is currently in alpha testing and is experimental. Its use may lead to incorrect entity behavior and crashes.
DO NOT report 1.20.1 or Neoforge problems to the original dev, only me!

## What is Async? 🤔

Async is a mod that enhances the performance of entity processing. The mod leverages multithreading, which allows multiple CPU cores to improve performance when handling a large number of entities.

### 💡 Key Benefits:

- ⚡ **Improved TPS**: Maintains stable tick times even with a large number of entities.
- 🚀 **Multithreading**: Utilizes multiple CPU cores for parallel entity processing.

### 📊 Performance Comparison (9000 Villagers)

| Configuration               | TPS  | MSPT   |
|-----------------------------|------|--------|
| **Lithium + Async**         | 20   | 41.8   |
| **Lithium (without Async)** | 4.4  | 225.4  |
| **Purpur**                  | 5.72 | 176.18 |

### 🛠️ Test Configuration

- **Processor**: AMD Ryzen 9 7950X3D
- **RAM**: 64 GB (16 GB allocated to the server)
- **Minecraft Version**: 1.21.4
- **Number of Entities**: 9000
- **Entity Type**: Villagers

<details>
<summary>Mod List</summary>

Concurrent Chunk Management Engine, Fabric API, FerriteCore, Lithium, ScalableLux, ServerCore, StackDeobfuscator, TT20 (TPS Fixer), Tectonic, Very Many Players, Fabric Carpet.

</details>

## 🔧 Commands
- `/async config toggle` — Enables or disables the mod in-game (no server restart required). Use this command to instantly see how Async improves your server.
- `/async config synchronizedEntities add` — Adds selected entity to synchronized processing.
- `/async config synchronizedEntities remove` — Removes selected entity from synchronized processing.
- `/async stats` — Displays the number of threads in use.
- `/async stats entity` — Shows the number of entities processed by Async in various worlds.

## 📥 Download

You can download the port from [GitHub](https://github.com/Bliss-tbh/Async-1.20.1/releases)

## 🔄 Minecraft Version Support

Full support for 1.20.1 Forge, Fabric and 1.21.1 Neoforge

## 📭 Feedback

My tracker for feedback and bug reports is available on GitHub:
[![Report issues on GitHub](https://img.shields.io/badge/Report%20issues%20on-GitHub-lightgrey)](https://github.com/Bliss-tbh/Async-1.20.1/issues)

## 🙌 Acknowledgements

This port is based on the code from [Async](https://github.com/AxalotLDev/Async), which in turn was based on [MCMTFabric](https://modrinth.com/mod/mcmtfabric), which in turn was based on [JMT-MCMT](https://github.com/jediminer543/JMT-MCMT). Huge thanks to Grider and jediminer543 for their invaluable contributions!
