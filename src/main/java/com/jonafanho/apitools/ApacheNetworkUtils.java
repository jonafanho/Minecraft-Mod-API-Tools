package com.jonafanho.apitools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public interface ApacheNetworkUtils {

	static JsonObject send(String url, String jsonKey, JsonObject json, InputStream inputStream, String fileName, String... headers) {
		try {
			final HttpPost request = new HttpPost(url);
			for (int i = 0; i < headers.length / 2; i++) {
				request.addHeader(headers[i * 2], headers[i * 2 + 1]);
			}
			request.setEntity(MultipartEntityBuilder.create()
					.addTextBody(jsonKey, json.toString(), ContentType.APPLICATION_JSON)
					.addPart("file", new InputStreamBody(inputStream, fileName))
					.build()
			);
			try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
				final HttpResponse response = httpClient.execute(request);
				if (response != null) {
					return new JsonParser().parse(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)).getAsJsonObject();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new JsonObject();
	}
}
