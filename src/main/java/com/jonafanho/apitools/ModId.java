package com.jonafanho.apitools;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModId {

	public final String modId;
	public final ModProvider modProvider;

	public ModId(String modId, ModProvider modProvider) {
		this.modId = modId;
		this.modProvider = modProvider;
	}

	public List<ModFile> getModFiles(String minecraftVersion, ModLoader modLoader, String curseForgeKey) {
		final List<ModFile> modFiles = new ArrayList<>();

		try {
			switch (modProvider) {
				case CURSE_FORGE:
					NetworkUtils.openConnectionSafeJson(NetworkUtils.urlBuilder(
							String.format("https://api.curseforge.com/v1/mods/%s/files", modId),
							"gameVersion", minecraftVersion,
							"modLoaderType", modLoader == null ? null : modLoader.name
					), jsonElement -> jsonElement.getAsJsonObject().getAsJsonArray("data").forEach(modElement -> ModFile.fromCurseForge(modElement.getAsJsonObject(), modFiles::add)), "x-api-key", curseForgeKey);
					break;
				case MODRINTH:
					NetworkUtils.openConnectionSafeJson(NetworkUtils.urlBuilder(
							String.format("https://api.modrinth.com/v2/project/%s/version", modId),
							"game_versions", minecraftVersion == null ? null : String.format("%%5B%%22%s%%22%%5D", minecraftVersion),
							"loaders", modLoader == null ? null : String.format("%%5B%%22%s%%22%%5D", modLoader.name)
					), jsonElement -> jsonElement.getAsJsonArray().forEach(modElement -> ModFile.fromModrinth(modElement.getAsJsonObject(), modFiles::add)));
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return modFiles;
	}

	public Mod getMod(String curseForgeKey) {
		final Mod[] mod = {null};

		try {
			switch (modProvider) {
				case CURSE_FORGE:
					NetworkUtils.openConnectionSafeJson(String.format("https://api.curseforge.com/v1/mods/%s", modId), jsonElement -> mod[0] = Mod.fromCurseForge(jsonElement.getAsJsonObject().getAsJsonObject("data")), "x-api-key", curseForgeKey);
					break;
				case MODRINTH:
					NetworkUtils.openConnectionSafeJson(String.format("https://api.modrinth.com/v2/project/%s", modId), jsonElement -> mod[0] = Mod.fromModrinth(jsonElement.getAsJsonObject()));
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mod[0];
	}

	public boolean uploadFile(String name, String modVersion, String changelog, Map<String, DependencyType> dependencies, ReleaseStatus releaseStatus, Set<String> minecraftVersions, Set<ModLoader> loaders, boolean featured, InputStream inputStream, String fileName, String apiKey) {
		final JsonObject dataObject = new JsonObject();

		switch (modProvider) {
			case CURSE_FORGE:
				final JsonArray versionArrayCurseForge = new JsonArray();
				minecraftVersions.forEach(minecraftVersion -> NetworkUtils.openConnectionSafeJson("https://api.curseforge.com/v1/minecraft/version/" + minecraftVersion, jsonElement -> versionArrayCurseForge.add(jsonElement.getAsJsonObject().getAsJsonObject("data").get("gameVersionId").getAsInt())));
				loaders.forEach(loader -> {
					if (loader == ModLoader.FABRIC || loader == ModLoader.FORGE) {
						versionArrayCurseForge.add(loader == ModLoader.FABRIC ? 7499 : 7498);
					}
				});

				final JsonArray dependencyArrayCurseForge = new JsonArray();
				dependencies.forEach((dependencyModId, dependencyType) -> {
					final JsonObject dependencyObject = new JsonObject();
					dependencyObject.addProperty("slug", dependencyModId);
					dependencyObject.addProperty("type", dependencyType == DependencyType.OPTIONAL ? "optionalDependency" : dependencyType == DependencyType.INCOMPATIBLE ? "incompatible" : dependencyType == DependencyType.EMBEDDED ? "embeddedLibrary" : "requiredDependency");
					dependencyArrayCurseForge.add(dependencyObject);
				});
				final JsonObject dependencyObject = new JsonObject();
				dependencyObject.add("projects", dependencyArrayCurseForge);

				dataObject.addProperty("changelog", changelog);
				dataObject.addProperty("changelogType", "markdown");
				dataObject.addProperty("displayName", modVersion);
				dataObject.add("gameVersions", versionArrayCurseForge);
				dataObject.addProperty("releaseType", releaseStatus.toString().toLowerCase());
				dataObject.add("relations", dependencyObject);

				final JsonObject curseForgeResponseObject = ApacheNetworkUtils.send("https://minecraft.curseforge.com/api/projects/266707/upload-file?token=" + apiKey, "metadata", dataObject, inputStream, fileName);
				System.out.println(curseForgeResponseObject);
				return curseForgeResponseObject.has("id");
			case MODRINTH:
				final JsonArray dependencyArrayModrinth = new JsonArray();
				dependencies.forEach((dependencyModId, dependencyType) -> {
					final JsonObject requiredDependencyObject = new JsonObject();
					requiredDependencyObject.addProperty("project_id", dependencyModId);
					requiredDependencyObject.addProperty("dependency_type", dependencyType.toString().toLowerCase());
					dependencyArrayModrinth.add(requiredDependencyObject);
				});

				final JsonArray versionArrayModrinth = new JsonArray();
				minecraftVersions.forEach(versionArrayModrinth::add);

				final JsonArray loaderArray = new JsonArray();
				loaders.forEach(loader -> loaderArray.add(loader.name));

				final JsonArray fileArray = new JsonArray();
				fileArray.add("file");

				dataObject.addProperty("name", name);
				dataObject.addProperty("version_number", modVersion);
				dataObject.addProperty("changelog", changelog);
				dataObject.add("dependencies", dependencyArrayModrinth);
				dataObject.add("game_versions", versionArrayModrinth);
				dataObject.addProperty("version_type", releaseStatus.toString().toLowerCase());
				dataObject.add("loaders", loaderArray);
				dataObject.addProperty("featured", featured);
				dataObject.addProperty("project_id", modId);
				dataObject.add("file_parts", fileArray);

				final JsonObject modrinthResponseObject = ApacheNetworkUtils.send("https://api.modrinth.com/v2/version", "data", dataObject, inputStream, fileName, "Authorization", apiKey);
				System.out.println(modrinthResponseObject);
				return modrinthResponseObject.has("error");
			default:
				return false;
		}
	}
}
