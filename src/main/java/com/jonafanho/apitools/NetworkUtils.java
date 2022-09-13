package com.jonafanho.apitools;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public interface NetworkUtils {

	static void openConnectionSafe(String url, Consumer<InputStream> callback, String... requestProperties) {
		try {
			final URLConnection urlConnection = new URL(url).openConnection();

			for (int i = 0; i < requestProperties.length / 2; i++) {
				urlConnection.setRequestProperty(requestProperties[2 * i], requestProperties[2 * i + 1]);
			}

			try (final InputStream inputStream = urlConnection.getInputStream()) {
				callback.accept(inputStream);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void openConnectionSafeJson(String url, Consumer<JsonElement> callback, String... requestProperties) {
		openConnectionSafe(url, inputStream -> {
			try (final InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
				callback.accept(new JsonParser().parse(inputStreamReader));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, requestProperties);
	}

	static String urlBuilder(String url, String... parameters) {
		boolean addedFirst = false;
		final StringBuilder stringBuilder = new StringBuilder(url);
		for (int i = 0; i < parameters.length / 2; i++) {
			final String key = parameters[i * 2];
			final String value = parameters[i * 2 + 1];
			if (value != null) {
				stringBuilder.append(addedFirst ? "&" : "?").append(key).append("=").append(value);
				addedFirst = true;
			}
		}
		return stringBuilder.toString();
	}
}
