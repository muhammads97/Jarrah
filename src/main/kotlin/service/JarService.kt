package com.jarrah.service

import com.jarrah.domain.Jar
import com.jarrah.domain.JarAccess
import com.jarrah.domain.JarTitle
import com.jarrah.infra.JarsRepository
import com.jarrah.infra.UsersRepository
import com.jarrah.utilities.Database
import java.time.Instant
import java.util.UUID

object JarService {
    fun createJar(userId: UUID, jarTitle: String): JarServiceResult {
        val (jar, jarAccess) = Jar.create(userId, JarTitle(jarTitle))
        Database.withTransaction { conn ->
            JarsRepository.create(jar, conn)
            JarsRepository.createOrUpdateAccess(jarAccess, conn)
        }
        return JarServiceResult.JarWithAccess(jar, jarAccess)
    }

    fun updateUserJarAccess(
        userId: UUID,
        jarId: UUID,
        accessorId: UUID,
        canView: Boolean,
        canAdd: Boolean,
        canSpend: Boolean
    ): JarServiceResult {
        return Database.withTransaction { conn ->
            val jar = JarsRepository.getById(jarId, conn)?: return@withTransaction JarServiceResult.JarNotFound
            if(jar.ownerId != userId) return@withTransaction JarServiceResult.ActionNotPermitted
            val accessor = UsersRepository.getById(accessorId, conn)?: return@withTransaction JarServiceResult.AccessorNotFound
            val jarAccess = JarAccess(
                userId = accessor.id,
                jarId = jar.id,
                canAdd = canAdd,
                canSpend = canSpend,
                canView = canView,
                updatedAt = Instant.now(),
                createdAt = Instant.now(),
            )
            JarsRepository.createOrUpdateAccess(jarAccess, conn)
            return@withTransaction JarServiceResult.JarWithAccess(jar, jarAccess)
        }
    }

    sealed interface JarServiceResult {
        data class JarWithAccess(val jar: Jar, val jarAccess: JarAccess): JarServiceResult
        object ActionNotPermitted : JarServiceResult
        object JarNotFound : JarServiceResult
        object AccessorNotFound : JarServiceResult
    }
}