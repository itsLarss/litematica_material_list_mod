package app.schematictracker.core;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

/** Erkennt, was beim Verbinden eingegeben wurde: Kopplungscode oder Freigabe-Link. */
public sealed interface TrackerLink {
	/** Kurzer Code von der Website, z. B. "K7QM-4XPA" (Server muss bekannt sein). */
	record Code(String code) implements TrackerLink {}

	/** Freigabe-Link https://server/s/<token>; enthält den Server gleich mit. */
	record ShareLink(String server, String token) implements TrackerLink {}

	Pattern CODE = Pattern.compile("[A-HJKMNP-Z2-9]{4}-?[A-HJKMNP-Z2-9]{4}");
	Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{24,64}");

	/** null, wenn die Eingabe weder Code noch Link ist. */
	static TrackerLink parse(String input) {
		String text = input == null ? "" : input.trim();
		String upper = text.toUpperCase(Locale.ROOT).replace(" ", "");
		if (CODE.matcher(upper).matches()) return new Code(upper.replace("-", ""));
		try {
			URI uri = URI.create(text);
			if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) return null;
			String path = uri.getPath() == null ? "" : uri.getPath();
			if (!path.startsWith("/s/")) return null;
			String token = path.substring(3).replaceAll("/+$", "");
			if (!TOKEN.matcher(token).matches() || uri.getHost() == null) return null;
			return new ShareLink(normalizeServer(uri.getScheme() + "://" + uri.getRawAuthority()), token);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** "example.com/" → "https://example.com"; null bei Unsinn. */
	static String normalizeServer(String input) {
		String text = input == null ? "" : input.trim();
		if (text.isEmpty()) return null;
		if (!text.contains("://")) text = "https://" + text;
		try {
			URI uri = URI.create(text);
			if (uri.getHost() == null) return null;
			if (!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme())) return null;
			return uri.getScheme() + "://" + uri.getRawAuthority();
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
