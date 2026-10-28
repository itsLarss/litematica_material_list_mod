package de.cozyverse.schematictracker.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Client für die API v1 der Website (siehe docs/api-v1.md im Website-Repo).
 * Alle Aufrufe laufen asynchron; der Token geht nur im Header X-Share-Token mit.
 */
public final class TrackerApi {
	private static final Gson GSON = new Gson();
	private static final Duration TIMEOUT = Duration.ofSeconds(10);

	private final HttpClient http;
	private final String userAgent;

	public TrackerApi(String userAgent) {
		// HTTP/1.1: bei http:// würde sonst ein h2c-Upgrade versucht, das viele Server (z. B. Node) abbrechen.
		this.http = HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(TIMEOUT)
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		this.userAgent = userAgent;
	}

	/** Fehler mit HTTP-Status (0 = keine Verbindung). */
	public static final class ApiException extends RuntimeException {
		public final int status;

		public ApiException(int status, String message) {
			super(message);
			this.status = status;
		}
	}

	public record Pairing(String token, String listId, String name, String role) {}

	public record ListResponse(String id, String name, String role, long version, boolean partial,
			List<TrackedItem> items) {}

	public record AmountChange(String id, int have, boolean farmed) {}

	public CompletableFuture<Pairing> pair(String server, String code) {
		return send(request(server, "/api/v1/pair", null).POST(body(Map.of("code", code))))
				.thenApply(json -> {
					JsonObject o = json.getAsJsonObject();
					return new Pairing(str(o, "token"), str(o, "listId"), str(o, "name"), str(o, "role"));
				});
	}

	/** Ganze Liste (since < 0) oder nur die Änderungen seit `since`. */
	public CompletableFuture<ListResponse> list(String server, String token, long since) {
		String path = since < 0 ? "/api/v1/list" : "/api/v1/list?since=" + since;
		return send(request(server, path, token).GET()).thenApply(json -> parseList(json.getAsJsonObject()));
	}

	public CompletableFuture<Void> saveAmounts(String server, String token, List<AmountChange> items, String actor) {
		JsonObject payload = new JsonObject();
		payload.add("items", GSON.toJsonTree(items));
		if (actor != null && !actor.isBlank()) payload.addProperty("actor", actor);
		return send(request(server, "/api/v1/list/items", token)
				.method("PATCH", HttpRequest.BodyPublishers.ofString(payload.toString()))).thenApply(json -> null);
	}

	/** claimedBy = null gibt das Material frei. */
	public CompletableFuture<Void> saveClaim(String server, String token, String itemId, String claimedBy, String actor) {
		JsonObject item = new JsonObject();
		item.addProperty("id", itemId);
		item.addProperty("claimedBy", claimedBy);
		JsonArray items = new JsonArray();
		items.add(item);
		JsonObject payload = new JsonObject();
		payload.add("items", items);
		if (actor != null && !actor.isBlank()) payload.addProperty("actor", actor);
		return send(request(server, "/api/v1/list/claims", token)
				.method("PATCH", HttpRequest.BodyPublishers.ofString(payload.toString()))).thenApply(json -> null);
	}

	static ListResponse parseList(JsonObject o) {
		List<TrackedItem> items = new ArrayList<>();
		int position = 0;
		for (JsonElement e : o.getAsJsonArray("items")) {
			JsonObject i = e.getAsJsonObject();
			items.add(new TrackedItem(
					str(i, "id"),
					str(i, "name"),
					str(i, "itemId"),
					i.get("total").getAsInt(),
					i.get("have").getAsInt(),
					i.get("stackSize").getAsInt(),
					i.get("farmed").getAsBoolean(),
					str(i, "claimedBy"),
					position++));
		}
		return new ListResponse(str(o, "id"), str(o, "name"), str(o, "role"), o.get("version").getAsLong(),
				o.get("partial").getAsBoolean(), items);
	}

	private HttpRequest.Builder request(String server, String path, String token) {
		HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(server + path))
				.timeout(TIMEOUT)
				.header("Accept", "application/json")
				.header("Content-Type", "application/json")
				.header("User-Agent", userAgent);
		if (token != null) b.header("X-Share-Token", token);
		return b;
	}

	private static HttpRequest.BodyPublisher body(Object value) {
		return HttpRequest.BodyPublishers.ofString(GSON.toJson(value));
	}

	private CompletableFuture<JsonElement> send(HttpRequest.Builder builder) {
		return http.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString())
				.handle((res, err) -> {
					if (err != null) throw new ApiException(0, err.getMessage() == null ? err.toString() : err.getMessage());
					JsonElement json;
					try {
						json = res.body() == null || res.body().isBlank() ? new JsonObject() : JsonParser.parseString(res.body());
					} catch (RuntimeException e) {
						json = new JsonObject();
					}
					if (res.statusCode() >= 400) {
						String message = json.isJsonObject() && json.getAsJsonObject().has("error")
								? json.getAsJsonObject().get("error").getAsString()
								: "HTTP " + res.statusCode();
						throw new ApiException(res.statusCode(), message);
					}
					return json;
				});
	}

	private static String str(JsonObject o, String key) {
		JsonElement e = o.get(key);
		return e == null || e.isJsonNull() ? null : e.getAsString();
	}
}
