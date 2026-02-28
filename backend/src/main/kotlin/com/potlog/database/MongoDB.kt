package com.potlog.database

import com.mongodb.MongoWriteException
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import com.potlog.models.PokerSession
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

object MongoDB {
    private val logger = LoggerFactory.getLogger(MongoDB::class.java)
    
    private lateinit var client: MongoClient
    private lateinit var database: MongoDatabase
    
    val sessions by lazy { database.getCollection<PokerSession>("sessions") }
    
    fun init(environment: ApplicationEnvironment) {
        val connectionString = environment.config.propertyOrNull("mongodb.connectionString")?.getString()
            ?: System.getenv("MONGODB_URI")
            ?: "mongodb://localhost:27017"
        val databaseName = environment.config.propertyOrNull("mongodb.database")?.getString()
            ?: System.getenv("MONGODB_DATABASE")
            ?: "potlog"
        
        logger.info("Connecting to MongoDB: $databaseName")
        
        client = MongoClient.create(connectionString)
        database = client.getDatabase(databaseName)
        
        runBlocking {
            ensureIndexes()
        }
        
        logger.info("MongoDB connection established")
    }
    
    private suspend fun ensureIndexes() {
        sessions.createIndex(
            Indexes.ascending("numericId"),
            IndexOptions().unique(true)
        )
        logger.info("Created unique index on numericId")
        
        sessions.createIndex(Indexes.ascending("status"))
        sessions.createIndex(Indexes.ascending("players.userId"))
        logger.info("Created additional indexes")
    }
    
    fun close() {
        client.close()
        logger.info("MongoDB connection closed")
    }
    
    fun isDuplicateKeyException(e: Exception): Boolean {
        return e is MongoWriteException && e.error.code == 11000
    }
}
