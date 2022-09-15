package com.jonafanho.apitools;

import com.google.gson.JsonObject;
import com.google.gson.internal.bind.util.ISO8601Utils;

import java.net.URL;
import java.text.ParsePosition;
import java.util.*;

public class Mod implements Comparable<Mod> {

	public final String name;
	public final String description;
	public final String imageUrl;
	public final int downloads;
	public final Date dateCreated;
	public final Date dateModified;
	public final Set<ModId> modIds = new HashSet<>();
	public final List<String> authors = new ArrayList<>();

	private Mod(String name, String description, String imageUrl, int downloads, Date dateCreated, Date dateModified) {
		this.name = name;
		this.description = description;
		this.imageUrl = imageUrl;
		this.downloads = downloads;
		this.dateCreated = dateCreated;
		this.dateModified = dateModified;
	}

	@Override
	public int compareTo(Mod mod) {
		return mod.downloads - downloads;
	}

	public static List<Mod> searchMods(String query, String minecraftVersion, ModLoader modLoader, String curseForgeKey) {
		final List<Mod> mods = new ArrayList<>();

		try {
			NetworkUtils.openConnectionSafeJson(NetworkUtils.urlBuilder(
					"https://api.curseforge.com/v1/mods/search",
					"gameId", "432",
					"sortField", "2",
					"sortOrder", "desc",
					"classId", "6",
					"searchFilter", query,
					"gameVersion", minecraftVersion,
					"modLoaderType", modLoader == null ? null : modLoader.name
			), jsonElement -> jsonElement.getAsJsonObject().getAsJsonArray("data").forEach(modElement -> addModToList(mods, fromCurseForge(modElement.getAsJsonObject()))), "x-api-key", curseForgeKey);
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			final List<String> facets = new ArrayList<>();
			if (minecraftVersion != null) {
				facets.add(String.format("%%5B%%22versions%%3A%s%%22%%5D", minecraftVersion));
			}
			if (modLoader != null) {
				facets.add(String.format("%%5B%%22categories%%3A%s%%22%%5D", modLoader.name));
			}
			facets.add("%5B%22project_type%3Amod%22%5D");

			NetworkUtils.openConnectionSafeJson(NetworkUtils.urlBuilder(
					"https://api.modrinth.com/v2/search",
					"query", query,
					"index", "relevance",
					"limit", "100",
					"facets", String.format("%%5B%s%%5D", String.join(",", facets))
			), jsonElement -> jsonElement.getAsJsonObject().getAsJsonArray("hits").forEach(modElement -> addModToList(mods, fromModrinth(modElement.getAsJsonObject()))));
		} catch (Exception e) {
			e.printStackTrace();
		}

		Collections.sort(mods);
		return mods;
	}

	public static Mod getModFromUrl(String urlString, String curseForgeKey) {
		final Mod[] mod = {null};

		try {
			final URL url = new URL(urlString);
			final String[] pathSplit = url.getPath().split("/");

			if (ModProvider.CURSE_FORGE.hostMatches(url)) {
				NetworkUtils.openConnectionSafeJson(NetworkUtils.urlBuilder(
						"https://api.curseforge.com/v1/mods/search",
						"gameId", "432",
						"classId", "6",
						"slug", pathSplit[3]
				), jsonElement -> jsonElement.getAsJsonObject().getAsJsonArray("data").forEach(modElement -> mod[0] = fromCurseForge(modElement.getAsJsonObject())), "x-api-key", curseForgeKey);
			} else if (ModProvider.MODRINTH.hostMatches(url)) {
				NetworkUtils.openConnectionSafeJson(String.format("https://api.modrinth.com/v2/project/%s", pathSplit[2]), jsonElement -> mod[0] = fromModrinth(jsonElement.getAsJsonObject()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mod[0];
	}

	protected static Mod fromCurseForge(JsonObject modObject) {
		try {
			final Mod mod = new Mod(
					modObject.get("name").getAsString(),
					modObject.get("summary").getAsString(),
					modObject.has("logo") && modObject.get("logo").isJsonObject() ? modObject.getAsJsonObject("logo").get("url").getAsString() : null,
					modObject.get("downloadCount").getAsInt(),
					ISO8601Utils.parse(modObject.get("dateCreated").getAsString(), new ParsePosition(0)),
					ISO8601Utils.parse(modObject.get("dateModified").getAsString(), new ParsePosition(0))
			);
			mod.modIds.add(new ModId(modObject.get("id").getAsString(), ModProvider.CURSE_FORGE));
			if (modObject.has("authors") && modObject.get("authors").isJsonArray()) {
				modObject.getAsJsonArray("authors").forEach(authorElement -> mod.authors.add(authorElement.getAsJsonObject().get("name").getAsString()));
			}
			return mod;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	protected static Mod fromModrinth(JsonObject modObject) {
		try {
			final Mod mod = new Mod(
					modObject.get("title").getAsString(),
					modObject.get("description").getAsString(),
					modObject.has("icon_url") ? modObject.get("icon_url").getAsString() : null,
					modObject.get("downloads").getAsInt(),
					ISO8601Utils.parse(tryGetString(modObject, "published", "date_created"), new ParsePosition(0)),
					ISO8601Utils.parse(tryGetString(modObject, "updated", "date_modified"), new ParsePosition(0))
			);
			mod.modIds.add(new ModId(tryGetString(modObject, "id", "project_id"), ModProvider.MODRINTH));
			if (modObject.has("author")) {
				mod.authors.add(modObject.get("author").getAsString());
			}
			return mod;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private static void addModToList(List<Mod> mods, Mod newMod) {
		final Mod existingMod = mods.stream().filter(checkMod -> cleanString(checkMod.name).equals(cleanString(newMod.name)) && cleanString(checkMod.description).equals(cleanString(newMod.description))).findFirst().orElse(null);

		if (existingMod == null) {
			mods.add(newMod);
		} else {
			mods.remove(existingMod);

			final Mod addMod = new Mod(
					existingMod.name,
					existingMod.description,
					existingMod.imageUrl,
					existingMod.downloads + newMod.downloads,
					getDate(existingMod.dateCreated, newMod.dateCreated, true),
					getDate(existingMod.dateModified, newMod.dateModified, false)
			);

			existingMod.authors.forEach(author -> addToListIfUnique(addMod.authors, author));
			newMod.authors.forEach(author -> addToListIfUnique(addMod.authors, author));
			addMod.modIds.addAll(existingMod.modIds);
			addMod.modIds.addAll(newMod.modIds);
			mods.add(addMod);
		}
	}

	private static void addToListIfUnique(List<String> dataList, String newData) {
		if (dataList.stream().noneMatch(checkData -> cleanString(checkData).equals(cleanString(newData)))) {
			dataList.add(newData);
		}
	}

	private static String tryGetString(JsonObject jsonObject, String... keys) {
		for (final String key : keys) {
			if (jsonObject.has(key)) {
				return jsonObject.get(key).getAsString();
			}
		}
		return "";
	}

	private static Date getDate(Date date1, Date date2, boolean getOlder) {
		return date1.before(date2) == getOlder ? date1 : date2;
	}

	private static String cleanString(String text) {
		return text.toLowerCase().replaceAll("[^a-z0-9]", "");
	}
}
