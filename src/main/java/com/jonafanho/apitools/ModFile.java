package com.jonafanho.apitools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.bind.util.ISO8601Utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class ModFile {

	public final String fileName;
	public final String url;
	public final String sha1;
	public final Date date;
	public final ReleaseStatus releaseStatus;
	public final Set<ModId> requiredDependencies;

	private ModFile(String fileName, String url, String sha1, Date date, ReleaseStatus releaseStatus, Set<ModId> requiredDependencies) {
		this.fileName = fileName;
		this.url = url;
		this.sha1 = sha1;
		this.date = date;
		this.releaseStatus = releaseStatus;
		this.requiredDependencies = requiredDependencies;
	}

	protected static void fromCurseForge(JsonObject fileObject, Consumer<ModFile> getModFile) {
		try {
			final int fileId = fileObject.get("id").getAsInt();
			final String fileName = fileObject.get("fileName").getAsString();

			final ReleaseStatus releaseStatus;
			switch (fileObject.get("releaseType").getAsInt()) {
				case 2:
					releaseStatus = ReleaseStatus.BETA;
					break;
				case 3:
					releaseStatus = ReleaseStatus.ALPHA;
					break;
				default:
					releaseStatus = ReleaseStatus.RELEASE;
					break;
			}

			final Set<ModId> dependencies = new HashSet<>();
			if (fileObject.has("dependencies")) {
				fileObject.getAsJsonArray("dependencies").forEach(dependency -> {
					final JsonObject dependencyObject = dependency.getAsJsonObject();
					if (dependencyObject.get("relationType").getAsInt() == 3) {
						dependencies.add(new ModId(dependencyObject.get("modId").getAsString(), ModProvider.CURSE_FORGE));
					}
				});
			}

			getModFile.accept(new ModFile(
					fileName,
					String.format("https://mediafiles.forgecdn.net/files/%s/%s/%s", fileId / 1000, fileId % 1000, URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())),
					fileObject.getAsJsonArray("hashes").get(0).getAsJsonObject().get("value").getAsString(),
					ISO8601Utils.parse(fileObject.get("fileDate").getAsString(), new ParsePosition(0)),
					releaseStatus,
					dependencies
			));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected static void fromModrinth(JsonObject jsonObject, Consumer<ModFile> getModFile) {
		try {
			final JsonObject fileObject = jsonObject.getAsJsonArray("files").get(0).getAsJsonObject();

			final Set<ModId> dependencies = new HashSet<>();
			if (jsonObject.has("dependencies")) {
				jsonObject.getAsJsonArray("dependencies").forEach(dependency -> {
					final JsonObject dependencyObject = dependency.getAsJsonObject();
					if (dependencyObject.get("dependency_type").getAsString().equals("required")) {
						final JsonElement dependencyProjectIdElement = dependencyObject.get("project_id");
						if (dependencyProjectIdElement.isJsonNull()) {
							final JsonElement dependencyVersionIdElement = dependencyObject.get("version_id");
							if (!dependencyVersionIdElement.isJsonNull()) {
								NetworkUtils.openConnectionSafeJson(String.format("https://api.modrinth.com/v2/version/%s", dependencyVersionIdElement.getAsString()), jsonElement -> dependencies.add(new ModId(jsonElement.getAsJsonObject().get("project_id").getAsString(), ModProvider.MODRINTH)));
							}
						} else {
							dependencies.add(new ModId(dependencyProjectIdElement.getAsString(), ModProvider.MODRINTH));
						}
					}
				});
			}

			final ReleaseStatus releaseStatus;
			switch (jsonObject.get("version_type").getAsString()) {
				case "beta":
					releaseStatus = ReleaseStatus.BETA;
					break;
				case "alpha":
					releaseStatus = ReleaseStatus.ALPHA;
					break;
				default:
					releaseStatus = ReleaseStatus.RELEASE;
					break;
			}

			getModFile.accept(new ModFile(
					fileObject.get("filename").getAsString(),
					fileObject.get("url").getAsString(),
					fileObject.getAsJsonObject("hashes").get("sha1").getAsString(),
					ISO8601Utils.parse(jsonObject.get("date_published").getAsString(), new ParsePosition(0)),
					releaseStatus,
					dependencies
			));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
