package com.integrity_shield

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class IntegrityShieldApplication

fun main(args: Array<String>) {
	runApplication<IntegrityShieldApplication>(*args)
}
