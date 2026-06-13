package com.example.chatplus.chat;

public class PlayerProfileData {
	private final String mother;
	private final String father;
	private final String familyName;
	private final String clan;
	private final String note;

	public PlayerProfileData(String mother, String father, String note) {
		this(mother, father, "", "", note);
	}

	public PlayerProfileData(String mother, String father, String familyName, String clan, String note) {
		this.mother = mother == null ? "" : mother;
		this.father = father == null ? "" : father;
		this.familyName = familyName == null ? "" : familyName;
		this.clan = clan == null ? "" : clan;
		this.note = note == null ? "" : note;
	}

	public String getMother() {
		return mother;
	}

	public String getFather() {
		return father;
	}

	public String getFamilyName() {
		return familyName;
	}

	public String getClan() {
		return clan;
	}

	public String getNote() {
		return note;
	}
}
