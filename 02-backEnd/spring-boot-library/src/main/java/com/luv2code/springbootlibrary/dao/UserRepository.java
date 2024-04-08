package com.luv2code.springbootlibrary.dao;

import com.luv2code.springbootlibrary.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
}
