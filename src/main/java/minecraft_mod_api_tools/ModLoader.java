package minecraft_mod_api_tools;

public enum ModLoader {

	FABRIC("fabric"), FORGE("forge");

	public final String name;

	ModLoader(String name) {
		this.name = name;
	}
}
