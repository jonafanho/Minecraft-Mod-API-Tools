package minecraft_mod_api_tools;

public class MinecraftModAPITools {

	private String minecraftVersion;
	private Loader loader;

	private final String curseForgeKey;

	public MinecraftModAPITools(String curseForgeKey) {
		this.curseForgeKey = curseForgeKey;
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			new MinecraftModAPITools(args[0]);
		}
	}
}
