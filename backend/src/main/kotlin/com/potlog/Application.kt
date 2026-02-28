package com.potlog

import com.potlog.database.MongoDB
import com.potlog.models.ErrorResponse
import com.potlog.routes.sessionRoutes
import com.potlog.services.SessionService
import com.potlog.services.SettlementService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    MongoDB.init(environment)
    
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
    
    install(CallLogging) {
        level = Level.INFO
    }
    
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("INTERNAL_ERROR", cause.message ?: "An unexpected error occurred")
            )
        }
    }
    
    val sessionService = SessionService()
    val settlementService = SettlementService(sessionService)
    
    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok", "service" to "potlog"))
        }
        
        sessionRoutes(sessionService, settlementService)
    }
    
    environment.monitor.subscribe(ApplicationStopped) {
        MongoDB.close()
    }
}
