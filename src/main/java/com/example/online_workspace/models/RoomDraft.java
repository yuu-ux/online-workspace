package com.example.online_workspace.models;

public class RoomDraft {

	private Long id;
	private final String name;
	private final String description;
	private final long createdBy;
	private final long categoryId;
	private final String workStyle;
	private final int maxMembers;

	public RoomDraft(
		String name,
		String description,
		long createdBy,
		long categoryId,
		String workStyle,
		int maxMembers
	) {
		this.name = name;
		this.description = description;
		this.createdBy = createdBy;
		this.categoryId = categoryId;
		this.workStyle = workStyle;
		this.maxMembers = maxMembers;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public long getCreatedBy() {
		return createdBy;
	}

	public long getCategoryId() {
		return categoryId;
	}

	public String getWorkStyle() {
		return workStyle;
	}

	public int getMaxMembers() {
		return maxMembers;
	}

}
