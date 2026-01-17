package com.example.ieum.controller

import com.example.ieum.entity.User
import com.example.ieum.repository.UserRepository
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(private val userRepository: UserRepository) {

    @PostMapping("/register")
    fun register(@RequestBody user: User): User {
        return userRepository.save(user)
    }

    @GetMapping("/{id}")
    fun getUser(@PathVariable id: Long): User? {
        return userRepository.findById(id).orElse(null)
    }
}