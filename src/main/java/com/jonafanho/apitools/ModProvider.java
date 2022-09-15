package com.jonafanho.apitools;

import java.net.URL;

public enum ModProvider {
	CURSE_FORGE("curseforge", "CurseForge", "curseforge.com"),
	MODRINTH("modrinth", "Modrinth", "modrinth.com");

	public final String id;
	public final String name;
	private final String host;

	ModProvider(String id, String name, String host) {
		this.id = id;
		this.name = name;
		this.host = host;
	}

	public boolean hostMatches(URL url) {
		return url.getHost().toLowerCase().endsWith(host);
	}
}
