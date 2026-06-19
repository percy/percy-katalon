---
name: katalon-install
description: "Step-by-step guide to install the Percy Katalon plugin. Covers prerequisites (Node.js, Percy CLI, Katalon Studio), downloading or building the JAR, copying to Plugins/ folder, and verifying the installation. Use when someone says 'install percy katalon', 'set up percy for katalon', 'how to install the plugin', or needs onboarding help."
---

# Percy Katalon Plugin — Installation Guide

Walk the user through installing the Percy Katalon plugin step by step.

## Prerequisites Check

First, verify the user has the required tools:

```bash
node --version    # Need Node.js for Percy CLI
npx @percy/cli --version   # Percy CLI
java -version     # Java 17+ for building from source
gradle --version  # Gradle for building from source
```

If any are missing, guide the user:
- **Node.js**: Install from [nodejs.org](https://nodejs.org)
- **Percy CLI**: `npm install -g @percy/cli`
- **Java 17**: Download [Amazon Corretto 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
- **Gradle**: `brew install gradle` (macOS) or [gradle.org](https://gradle.org/install/)
- **Katalon Studio**: Download from [katalon.com/download](https://katalon.com/download) (free Community Edition)

## Step 1: Get the Plugin JAR

Ask the user which method they prefer:

**Option A: Download release**
- Direct them to GitHub Releases: `https://github.com/percy/percy-katalon/releases`
- Download `percy-katalon-<version>.jar`

**Option B: Build from source**
```bash
git clone https://github.com/percy/percy-katalon.git
cd percy-katalon
export JAVA_HOME=/path/to/java-17
gradle shadowJar
# Output: build/libs/percy-katalon-1.0.0.jar
```

## Step 2: Install in Katalon Project

```bash
cp percy-katalon-1.0.0.jar /path/to/your-katalon-project/Plugins/
```

Tell the user: Restart Katalon Studio after copying the JAR.

## Step 3: Get Percy Token

1. Go to [percy.io](https://percy.io)
2. Create or select a project
3. Copy the `PERCY_TOKEN` from Project Settings
4. Set in terminal: `export PERCY_TOKEN=<your-token>`

## Step 4: Verify Installation

Have the user create a simple test in Katalon Studio:

```groovy
WebUI.openBrowser('https://example.com')
CustomKeywords.'com.percy.katalon.PercyKeywords.percySnapshot'('Installation Test')
WebUI.closeBrowser()
```

If the keyword is not found → JAR not loaded. Check:
- JAR is in `Plugins/` folder
- Katalon was restarted
- Project > Settings > Plugins > "Katalon Store and Local"

If the keyword runs but snapshot is skipped → Percy CLI not running. That's OK — confirms the plugin is loaded and gracefully degrades.

## Success Criteria

- Plugin JAR in `Plugins/` folder
- `percySnapshot` keyword recognized by Katalon
- No errors when running a test (even without Percy CLI)
