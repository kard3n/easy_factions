# ⚔️ Easy Factions

> **Player Alliances Made Easy**
> Create factions, manage ranks, and forge alliances with ease.

**Easy Factions** is a lightweight Minecraft mod designed to streamline the clan experience. Whether you are running a casual SMP or a competitive PvP server, this mod allows players to easily organize into teams and forge alliances.

---

## ✨ Key Features

### 👥 Create & Conquer
Start your own faction in seconds. Inviting and managing members is easy and intuitive

### 🛡️ Friendly Fire
Toggle **Friendly Fire** on or off for your faction. No more accidental sword swings hitting your teammates during a raid!
-> Can be disabled via configuration.

### 🤝 Strategic Alliances
War is better with friends. Forge alliances with other factions to coordinate attacks.
* **Synchronized PvP:** You can fight alongside allied factions without accidental friendly fire!

### 👮 Rank Management
Delegate authority to keep your faction running smoothly.
* **Officers:** Promote trusted members to **Officer** status.
* **Permissions:** Officers have the power to invite new recruits or kick trouble-makers.

---

## 💻 Commands

**Faction commands:**
| Command | Description |
| :--- | :--- |
| `/faction create <name>` | Create a new faction. |
| `/faction invite <player>` | Invite a player to your faction. |
| `/faction join <name>` | Accept an invitation to join a faction. |
| `/faction leave` | Leave your current faction. If the owner leaves, the faction is abandoned |
| `/faction addOfficer <player>` | Promote a member to Officer. |
| `/faction kick <player>` | Kick a member (Officers/Leader only). |

**Alliance commands:**
Can only be executed by the owner of a faction.
| Command | Description |
| :--- | :--- |
| `/alliance create <name>` | Create a new alliance with the given name |
| `/alliance leave` | Leave your current alliance. Restricted to the faction owner |
| `/alliance invite <factionName>` | Invite a new faction to the alliance |
| `/alliance join <factionName>` | Accept an invitation to an alliance |
---

## 🔧 Configuration
Server owners can tweak **Easy Factions** to fit their gameplay style via the config file.

| Option              | Description                                                      |
|:--------------------|:-----------------------------------------------------------------|
| `"maxFactionSize`   | Sets the maximum number of players allowed in one faction.       |
| `maxAllianceSize`   | Limits how many factions an alliance can contain.                |
| `forceFriendlyFire` | If set to `true`, friendly fire will be enabled for all factions |