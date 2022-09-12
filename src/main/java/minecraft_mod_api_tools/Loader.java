package minecraft_mod_api_tools;

public enum Loader {

	FABRIC("fabric"), FORGE("forge");

	public final String name;

	Loader(String name) {
		this.name = name;
	}
}
