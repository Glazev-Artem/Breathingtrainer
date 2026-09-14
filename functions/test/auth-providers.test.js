"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const {
  ProviderAuthError,
  ProviderUnavailableError,
  verifyYandexToken,
  verifyVkAuthorizationCode,
  verifyVkToken,
} = require("../auth-providers");

function jsonResponse(status, body) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
  };
}

test("Yandex token is accepted only for the configured OAuth client", async () => {
  const fetchImpl = async (url, options) => {
    assert.equal(url, "https://login.yandex.ru/info?format=json");
    assert.equal(options.method, "GET");
    assert.equal(options.headers.Authorization, "OAuth valid-token");
    return jsonResponse(200, {
      id: "42",
      psuid: "app-scoped-42",
      client_id: "expected-client",
      display_name: "Test User",
    });
  };

  const identity = await verifyYandexToken("valid-token", "expected-client", fetchImpl);
  assert.deepEqual(identity, {
    providerUserId: "app-scoped-42",
    displayName: "Test User",
  });
});

test("Yandex token for a different application is rejected", async () => {
  const fetchImpl = async () => jsonResponse(200, {
    id: "42",
    client_id: "attacker-client",
  });

  await assert.rejects(
    verifyYandexToken("valid-token", "expected-client", fetchImpl),
    ProviderAuthError,
  );
});

test("VK token is checked by the official user_info endpoint", async () => {
  const fetchImpl = async (url, options) => {
    assert.equal(url, "https://id.vk.com/oauth2/user_info?client_id=12345");
    assert.equal(options.method, "POST");
    assert.equal(options.body.get("access_token"), "valid-vk-token");
    return jsonResponse(200, {
      user: { user_id: "77", first_name: "Test", last_name: "User" },
    });
  };

  const identity = await verifyVkToken("valid-vk-token", "12345", fetchImpl);
  assert.deepEqual(identity, {
    providerUserId: "77",
    displayName: "Test User",
  });
});

test("VK authorization code is exchanged with PKCE and matching state", async () => {
  let call = 0;
  const fetchImpl = async (url, options) => {
    call += 1;
    if (call === 1) {
      const parsed = new URL(url);
      assert.equal(parsed.origin + parsed.pathname, "https://id.vk.com/oauth2/auth");
      assert.equal(parsed.searchParams.get("grant_type"), "authorization_code");
      assert.equal(parsed.searchParams.get("redirect_uri"), "vk12345://vk.com");
      assert.equal(parsed.searchParams.get("client_id"), "12345");
      assert.equal(parsed.searchParams.get("device_id"), "device-7");
      assert.equal(parsed.searchParams.get("state"), "state_value_12345678901234567890");
      assert.equal(options.body.get("code"), "one-time-code");
      return jsonResponse(200, {
        access_token: "vk-access-token",
        state: "state_value_12345678901234567890",
      });
    }

    assert.equal(url, "https://id.vk.com/oauth2/user_info?client_id=12345");
    assert.equal(options.body.get("access_token"), "vk-access-token");
    return jsonResponse(200, {
      user: { user_id: "77", first_name: "Test", last_name: "User" },
    });
  };

  const identity = await verifyVkAuthorizationCode(
    {
      authorizationCode: "one-time-code",
      deviceId: "device-7",
      codeVerifier: "a".repeat(64),
      state: "state_value_12345678901234567890",
    },
    "12345",
    fetchImpl,
  );

  assert.equal(call, 2);
  assert.deepEqual(identity, {
    providerUserId: "77",
    displayName: "Test User",
  });
});

test("VK authorization response with a different state is rejected", async () => {
  const fetchImpl = async () => jsonResponse(200, {
    access_token: "vk-access-token",
    state: "attacker_state_12345678901234567890",
  });

  await assert.rejects(
    verifyVkAuthorizationCode(
      {
        authorizationCode: "one-time-code",
        deviceId: "device-7",
        codeVerifier: "a".repeat(64),
        state: "expected_state_12345678901234567890",
      },
      "12345",
      fetchImpl,
    ),
    ProviderAuthError,
  );
});

test("provider 4xx is treated as invalid credentials", async () => {
  const fetchImpl = async () => jsonResponse(401, {});
  await assert.rejects(
    verifyVkToken("invalid-token", "12345", fetchImpl),
    ProviderAuthError,
  );
});

test("provider 5xx does not incorrectly invalidate credentials", async () => {
  const fetchImpl = async () => jsonResponse(503, {});
  await assert.rejects(
    verifyVkToken("valid-token", "12345", fetchImpl),
    ProviderUnavailableError,
  );
});

test("malformed tokens are rejected before contacting a provider", async () => {
  let called = false;
  const fetchImpl = async () => {
    called = true;
    return jsonResponse(200, {});
  };

  await assert.rejects(
    verifyYandexToken("bad\ntoken", "expected-client", fetchImpl),
    ProviderAuthError,
  );
  assert.equal(called, false);
});
