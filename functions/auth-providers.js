"use strict";

const MAX_TOKEN_LENGTH = 8192;
const MAX_AUTH_CODE_LENGTH = 4096;
const MAX_DEVICE_ID_LENGTH = 512;
const PKCE_VALUE_PATTERN = /^[A-Za-z0-9._~-]{43,128}$/;
const STATE_PATTERN = /^[A-Za-z0-9_-]{20,256}$/;
const REQUEST_TIMEOUT_MS = 8000;

class ProviderAuthError extends Error {
  constructor(message = "Provider rejected the token") {
    super(message);
    this.name = "ProviderAuthError";
  }
}

class ProviderUnavailableError extends Error {
  constructor(message = "Provider is temporarily unavailable") {
    super(message);
    this.name = "ProviderUnavailableError";
  }
}

function validateAccessToken(accessToken) {
  if (
    typeof accessToken !== "string" ||
    accessToken.length === 0 ||
    accessToken.length > MAX_TOKEN_LENGTH ||
    /[\u0000-\u001f\u007f]/.test(accessToken)
  ) {
    throw new ProviderAuthError("Malformed access token");
  }
  return accessToken;
}

function validateClientId(clientId, provider) {
  if (typeof clientId !== "string" || clientId.trim().length === 0) {
    throw new Error(`${provider} client ID is not configured`);
  }
  return clientId.trim();
}

function validateVkAuthorizationProof({ authorizationCode, deviceId, codeVerifier, state }) {
  if (
    typeof authorizationCode !== "string" ||
    authorizationCode.length === 0 ||
    authorizationCode.length > MAX_AUTH_CODE_LENGTH ||
    /[\u0000-\u001f\u007f]/.test(authorizationCode) ||
    typeof deviceId !== "string" ||
    deviceId.length === 0 ||
    deviceId.length > MAX_DEVICE_ID_LENGTH ||
    /[\u0000-\u001f\u007f]/.test(deviceId) ||
    typeof codeVerifier !== "string" ||
    !PKCE_VALUE_PATTERN.test(codeVerifier) ||
    typeof state !== "string" ||
    !STATE_PATTERN.test(state)
  ) {
    throw new ProviderAuthError("Malformed VK authorization proof");
  }
  return { authorizationCode, deviceId, codeVerifier, state };
}

function cleanDisplayName(value) {
  if (typeof value !== "string") return null;
  const cleaned = value.replace(/[\u0000-\u001f\u007f]/g, " ").trim();
  return cleaned.length > 0 ? cleaned.slice(0, 100) : null;
}

async function readProviderJson(response) {
  if (!response || !response.ok) {
    if (response && response.status >= 400 && response.status < 500) {
      throw new ProviderAuthError();
    }
    throw new ProviderUnavailableError();
  }

  try {
    return await response.json();
  } catch {
    throw new ProviderUnavailableError("Provider returned invalid JSON");
  }
}

async function providerFetch(fetchImpl, url, options) {
  try {
    return await fetchImpl(url, {
      ...options,
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
  } catch (error) {
    if (error instanceof ProviderAuthError || error instanceof ProviderUnavailableError) {
      throw error;
    }
    throw new ProviderUnavailableError();
  }
}

async function verifyYandexToken(accessToken, expectedClientId, fetchImpl = fetch) {
  const token = validateAccessToken(accessToken);
  const clientId = validateClientId(expectedClientId, "Yandex");
  const response = await providerFetch(
    fetchImpl,
    "https://login.yandex.ru/info?format=json",
    {
      method: "GET",
      headers: {
        Authorization: `OAuth ${token}`,
        Accept: "application/json",
      },
    },
  );
  const data = await readProviderJson(response);

  if (String(data.client_id || "") !== clientId || !data.id) {
    throw new ProviderAuthError();
  }

  return {
    providerUserId: String(data.psuid || data.id),
    displayName: cleanDisplayName(data.display_name || data.real_name || data.login),
  };
}

async function verifyVkToken(accessToken, expectedClientId, fetchImpl = fetch) {
  const token = validateAccessToken(accessToken);
  const clientId = validateClientId(expectedClientId, "VK");
  const body = new URLSearchParams({ access_token: token });
  const response = await providerFetch(
    fetchImpl,
    `https://id.vk.com/oauth2/user_info?client_id=${encodeURIComponent(clientId)}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json",
      },
      body,
    },
  );
  const data = await readProviderJson(response);
  const user = data && data.user;

  if (!user || !user.user_id) {
    throw new ProviderAuthError();
  }

  return {
    providerUserId: String(user.user_id),
    displayName: cleanDisplayName([user.first_name, user.last_name].filter(Boolean).join(" ")),
  };
}

async function verifyVkAuthorizationCode(proof, expectedClientId, fetchImpl = fetch) {
  const { authorizationCode, deviceId, codeVerifier, state } = validateVkAuthorizationProof(proof);
  const clientId = validateClientId(expectedClientId, "VK");
  const redirectUri = `vk${clientId}://vk.com`;
  const query = new URLSearchParams({
    grant_type: "authorization_code",
    redirect_uri: redirectUri,
    client_id: clientId,
    code_verifier: codeVerifier,
    device_id: deviceId,
    state,
  });
  const response = await providerFetch(
    fetchImpl,
    `https://id.vk.com/oauth2/auth?${query.toString()}`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json",
      },
      body: new URLSearchParams({ code: authorizationCode }),
    },
  );
  const tokenData = await readProviderJson(response);
  if (!tokenData.access_token || tokenData.state !== state) {
    throw new ProviderAuthError("VK authorization response did not match the request");
  }
  return verifyVkToken(tokenData.access_token, clientId, fetchImpl);
}

module.exports = {
  ProviderAuthError,
  ProviderUnavailableError,
  verifyYandexToken,
  verifyVkAuthorizationCode,
  verifyVkToken,
};
