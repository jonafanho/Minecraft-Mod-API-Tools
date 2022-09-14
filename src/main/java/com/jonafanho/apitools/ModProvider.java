package com.jonafanho.apitools;

public enum ModProvider {
	CURSE_FORGE("curseforge", "CurseForge"), MODRINTH("modrinth", "Modrinth");

	public final String id;
	public final String name;

	ModProvider(String id, String name) {
		this.id = id;
		this.name = name;
	}
}
