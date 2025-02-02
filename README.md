# Me(e) (premium level-role) bypasser

This Discord Bot bypasses a premium feature of the Bot Mee6, that you can use it for free.

You can select roles, that are given to users that reach a certain Mee6-Level.

* In order to add roles, you can execute the command `/add <Level number> <Role ID>`
* In order to remove roles, you can run `/remove <Level number>`
* In order to show all assigned roles, you can use `/show`
* In order to get the id of a role, you can execute `/id <Role name>`
* In order to configure whether roles should be removed if someone reaches a higher role, you do that with `/toggle`.
* Users get the roles when executing `!rank` or `/rank`

Do not include greater than/lower than signs (`<>`) in the commands.

### Ratelimits

This bot only allows one rank update per guild per second in order to prevent high resource consumption due to OCR.

## Build the bot

### Requirements
* JDK 8 or later (You can download it [here](https://adoptopenjdk.net/))
* [Maven](https://maven.apache.org/download.cgi)

### Building
* Clone the project
* After installing JDK8+ and Maven, it is possible to create a runnable JAR using the command `mvn package`<br/>
  This creates a file called `mee-bypasser.jar` in a directory named `target`

### Running
* Obtain the file `mee-bypasser.jar` (see [Building](https://github.com/JDiscordBots/Me-e--bypasser#building))
* Create a text file called `.token` and copy your bot token (from <https://discord.com/developers/applications/me>) in this file.
* Run the command `java -jar mee-bypasser.jar` in the directory with those files

### IDE Setup
* Clone the project
* Import it in your IDE of choice as a maven project
* Create a text file called `.token` in the root directory of the project and copy your bot token (from <https://discord.com/developers/applications/me>) in this file.
* Run the class `io.github.jdiscordbots.mee.bypasser.MeeBypasser`

## Privacy

Me(e)-bypasser stores the following information:
- IDs of roles that should be awarded (configurable with `/add` and `/remove`, viewable with `/show` and `/list`)
- levels at which roles should be awarded (configurable with `/add` and `/remove`, viewable with `/show` and `/list`)
- whether previous roles should be removed whenever a user reaches the next role (configurable with `/toggle`, viewable with `/show` and `/list`)
- the last time when a rank update has been requested (required for rate limits)

## Public Instance

Click [here](https://discord.com/api/oauth2/authorize?client_id=644830792845099009&permissions=268520448&scope=bot%20applications.commands) in order to invite an instance of the Bot.

We do not guarantee uptime or functionality of the bot and/or any instance of the bot in any way.

Other bots offer that feature with their own leveling system for free, you can try those too.
