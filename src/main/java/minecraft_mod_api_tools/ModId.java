package minecraft_mod_api_tools;

import java.util.ArrayList;
import java.util.List;

public class ModId {

	private final String modId;
	private final ModProvider modProvider;

	public ModId(String modId, ModProvider modProvider) {
		this.modId = modId;
		this.modProvider = modProvider;
	}

	public List<ModFile> getModFiles(String minecraftVersion, ModLoader modLoader, String curseForgeKey) {
		final List<ModFile> modFiles = new ArrayList<>();

		switch (modProvider) {
			case CURSE_FORGE:
				Network.openConnectionSafeJson(Network.urlBuilder(
						String.format("https://api.curseforge.com/v1/mods/%s/files", modId),
						"gameVersion", minecraftVersion,
						"modLoaderType", modLoader == null ? null : modLoader.name
				), jsonElement -> jsonElement.getAsJsonObject().getAsJsonArray("data").forEach(modElement -> ModFile.fromCurseForge(modElement.getAsJsonObject(), modFiles::add)), "x-api-key", curseForgeKey);
				break;
			case MODRINTH:
				Network.openConnectionSafeJson(Network.urlBuilder(
						String.format("https://api.modrinth.com/v2/project/%s/version", modId),
						"game_versions", minecraftVersion == null ? null : String.format("%%5B%%22%s%%22%%5D", minecraftVersion),
						"loaders", modLoader == null ? null : String.format("%%5B%%22%s%%22%%5D", modLoader.name)
				), jsonElement -> jsonElement.getAsJsonArray().forEach(modElement -> ModFile.fromModrinth(modElement.getAsJsonObject(), modFiles::add)));
				break;
		}

		return modFiles;
	}
}
