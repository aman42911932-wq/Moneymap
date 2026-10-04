package com.moneymap.service;

import com.moneymap.dao.AdminDAO;
import com.moneymap.dao.UserDAO;
import com.moneymap.model.Admin;
import com.moneymap.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class AuthService {
    private final UserDAO userDAO = new UserDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    public User authenticateUser(String email, String password) throws SQLException {
        User user = userDAO.findByEmail(email);
        if (user != null) {
            if (BCrypt.checkpw(password, user.getPasswordHash()) || user.getPasswordHash().equals(password)) {
                return user;
            }
        }
        return null;
    }

    public Admin authenticateAdmin(String email, String password) throws SQLException {
        Admin admin = adminDAO.findByEmail(email);
        if (admin != null) {
            if (BCrypt.checkpw(password, admin.getPasswordHash()) || admin.getPasswordHash().equals(password)) {
                return admin;
            }
        }
        return null;
    }

    public User registerUser(String name, String email, String phone, String password) throws SQLException {
        String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(hashed);
        user.setActive(true);
        return userDAO.create(user);
    }
}
