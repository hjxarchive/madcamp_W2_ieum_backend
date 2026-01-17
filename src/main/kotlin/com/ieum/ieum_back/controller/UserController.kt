package com.ieum.ieum_back.controller

import com.ieum.ieum_back.entity.User
import com.ieum.ieum_back.repository.UserRepository
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