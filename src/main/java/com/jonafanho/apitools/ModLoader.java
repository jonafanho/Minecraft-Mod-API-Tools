package com.jonafanho.apitools;

public enum ModLoader {

	FABRIC("fabric"), FORGE("forge");

	public final String name;

	ModLoader(String name) {
		this.name = name;
	}
}
