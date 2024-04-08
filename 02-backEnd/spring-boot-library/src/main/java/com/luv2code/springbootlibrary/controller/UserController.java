package com.luv2code.springbootlibrary.controller;


import com.luv2code.springbootlibrary.dao.UserRepository;
import com.luv2code.springbootlibrary.entity.User;
import com.luv2code.springbootlibrary.utils.ExtractJWT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("https://localhost:3000")
@RestController
@RequestMapping("/api")
public class UserController {


    @Autowired
    private UserRepository userRepository;


    @PostMapping("/user")
    User newUser(@RequestBody User newUser){
        return  userRepository.save(newUser);
    }

    @GetMapping("/secure/users")
    public List<User> getAllUsers(@RequestHeader(value = "Authorization") String token) throws Exception {
        String admin = ExtractJWT.payloadJWTExtraction(token, "\"userType\"");
        if (admin == null || !admin.equals("admin")) {
            throw new Exception("Administration page only");
        }
        return userRepository.findAll();
    }


    @DeleteMapping("/secure/user/{id}")
    public void deleteBook(@RequestHeader(value = "Authorization") String token,
                           @PathVariable Long id) throws Exception {
        String admin = ExtractJWT.payloadJWTExtraction(token, "\"userType\"");
        if (admin == null || !admin.equals("admin")) {
            throw new Exception("Administration page only");
        }
        userRepository.deleteById(id);
    }


}


