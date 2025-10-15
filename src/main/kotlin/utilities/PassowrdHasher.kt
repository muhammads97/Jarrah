package com.jarrah.utilities

import de.mkammerer.argon2.Argon2Factory

class PassowrdHasher (
    private val iterations: Int = 3,
    private val memoryKb: Int = 65536,
    private val parallelism: Int = 1
) {
    private val argon2 = Argon2Factory.create()

    fun hash(plain: String): String {
        return argon2.hash(iterations, memoryKb, parallelism, plain.toCharArray())
    }

    fun verify(hash: String, plain: String): Boolean {
        return argon2.verify(hash, plain.toCharArray())
    }
}