package com.example.online_workspace.repositories;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.example.online_workspace.models.RoomCategory;

@Mapper
public interface RoomCategoryRepository {

	@Select("""
		SELECT c.id, c.name, c.description, c.sort_order
		FROM room_categories c
		JOIN room_category_statuses s ON s.id = c.status_id
		WHERE s.code = 'ACTIVE'
		ORDER BY c.sort_order, c.id
		""")
	List<RoomCategory> findActive();
}
