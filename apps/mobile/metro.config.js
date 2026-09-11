const path = require("path");

/** @type {import('expo/metro-config').MetroConfig} */
const { getDefaultConfig } = require("expo/metro-config");

const projectRoot = __dirname;
const workspaceRoot = path.resolve(projectRoot, "../..");
const sharedRoot = path.resolve(workspaceRoot, "shared");
// shared/contracts.ts imports ../contracts/deployment.json
const contractsRoot = path.resolve(workspaceRoot, "contracts");

const config = getDefaultConfig(projectRoot);

// Watch protocol sources + deployment JSON — not the entire monorepo.
config.watchFolders = [sharedRoot, contractsRoot];
config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, "node_modules"),
];
// Keep mobile node_modules authoritative so Expo SDK packages don't collide
// with the web app's dependencies at the repo root.
config.resolver.disableHierarchicalLookup = true;

module.exports = config;
