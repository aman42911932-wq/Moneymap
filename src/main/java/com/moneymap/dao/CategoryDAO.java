package com.moneymap.dao;

import com.moneymap.config.DBConnection;
import com.moneymap.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CategoryDAO {

    public List<Category> findAll() throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT category_id, name, type, icon, is_active FROM category ORDER BY type ASC, category_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public List<Category> findByType(String type) throws SQLException {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT category_id, name, type, icon, is_active FROM category WHERE LOWER(type) = LOWER(?) AND is_active = TRUE ORDER BY category_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    public Category findById(int categoryId) throws SQLException {
        String sql = "SELECT category_id, name, type, icon, is_active FROM category WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Category findByNameAndType(String name, String type) throws SQLException {
        if (name == null || name.trim().isEmpty()) return null;
        String sql = "SELECT category_id, name, type, icon, is_active FROM category WHERE LOWER(name) = LOWER(?) AND LOWER(type) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            ps.setString(2, type.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        // Fallback: match by prefix or substring
        String fallbackSql = "SELECT category_id, name, type, icon, is_active FROM category WHERE LOWER(name) LIKE LOWER(?) AND LOWER(type) = LOWER(?) LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(fallbackSql)) {
            ps.setString(1, "%" + name.trim() + "%");
            ps.setString(2, type.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Category create(Category cat) throws SQLException {
        String sql = "INSERT INTO category (name, type, icon, is_active) VALUES (?, ?, ?, ?) RETURNING category_id";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cat.getName());
            ps.setString(2, cat.getType().toLowerCase());
            ps.setString(3, cat.getIcon() != null ? cat.getIcon() : "tag");
            ps.setBoolean(4, true);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cat.setCategoryId(rs.getInt("category_id"));
                    cat.setActive(true);
                    return cat;
                }
            }
        }
        return cat;
    }

    public boolean updateStatus(int categoryId, boolean isActive) throws SQLException {
        String sql = "UPDATE category SET is_active = ? WHERE category_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, isActive);
            ps.setInt(2, categoryId);
            return ps.executeUpdate() > 0;
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(
                rs.getInt("category_id"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("icon"),
                rs.getBoolean("is_active")
        );
    }
}
