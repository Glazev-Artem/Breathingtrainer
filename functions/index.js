"use strict";

const { createHash } = require("node:crypto");
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { logger } = require("firebase-functions");
const { defineString } = require("firebase-functions/params");
const { onRequest } = require("firebase-functions/v2/https");
const {
  ProviderAuthError,
  ProviderUnavailableError,
  verifyYandexToken,
  verifyVkAuthorizationCode,
  verifyVkToken,
} = require("./auth-providers");

initializeApp();

const yandexClientId = defineString("YANDEX_CLIENT_ID");
const vkClientId = defineString("VK_CLIENT_ID");

function firebaseUid(provider, providerUserId) {
  const digest = createHash("sha256")
    .update(provider)
    .update("\0")
    .update(providerUserId)
    .digest("base64url");
  return `${provider}:${digest}`;
}

function sendJson(res, status, body) {
  res.set("Cache-Control", "no-store");
  res.status(status).json(body);
}

exports.exchangeExternalToken = onRequest(
  {
    region: "europe-west1",
    cors: false,
    invoker: "public",
    timeoutSeconds: 15,
    memory: "256MiB",
    minInstances: 0,
    maxInstances: 5,
    concurrency: 20,
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.set("Allow", "POST");
      sendJson(res, 405, { error: "method_not_allowed" });
      return;
    }

    if (!req.is("application/json")) {
      sendJson(res, 415, { error: "unsupported_media_type" });
      return;
    }

    const provider = req.body && req.body.provider;
    const accessToken = req.body && req.body.accessToken;
    if (provider !== "yandex" && provider !== "vk") {
      sendJson(res, 400, { error: "unsupported_provider" });
      return;
    }

    try {
      let identity;
      if (provider === "yandex") {
        identity = await verifyYandexToken(accessToken, yandexClientId.value());
      } else if (typeof accessToken === "string" && accessToken.length > 0) {
        // Keep compatibility with already installed app versions during rollout.
        identity = await verifyVkToken(accessToken, vkClientId.value());
      } else {
        identity = await verifyVkAuthorizationCode(
          {
            authorizationCode: req.body && req.body.authorizationCode,
            deviceId: req.body && req.body.deviceId,
            codeVerifier: req.body && req.body.codeVerifier,
            state: req.body && req.body.state,
          },
          vkClientId.value(),
        );
      }
      const uid = firebaseUid(provider, identity.providerUserId);
      const customToken = await getAuth().createCustomToken(uid, {
        externalProvider: provider,
      });

      sendJson(res, 200, {
        firebaseCustomToken: customToken,
        displayName: identity.displayName,
      });
    } catch (error) {
      if (error instanceof ProviderAuthError) {
        logger.warn("External identity token was rejected", { provider });
        sendJson(res, 401, { error: "invalid_credentials" });
        return;
      }
      if (error instanceof ProviderUnavailableError) {
        logger.error("External identity provider is unavailable", { provider });
        sendJson(res, 503, { error: "provider_unavailable" });
        return;
      }

      logger.error("External token exchange failed", { provider, error });
      sendJson(res, 500, { error: "internal_error" });
    }
  },
);
