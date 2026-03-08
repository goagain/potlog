package com.potlog.routes

import com.potlog.models.*
import com.potlog.services.SessionService
import com.potlog.services.SettlementService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.sessionRoutes(sessionService: SessionService, settlementService: SettlementService) {
    
    route("/api/sessions") {
        
        post {
            val request = call.receive<CreateSessionRequest>()
            val (session, adminPassword) = sessionService.createSession(request.stakes, request.adminOnly)
            call.respond(
                HttpStatusCode.Created,
                CreateSessionResponse(session.numericId, session, adminPassword)
            )
        }
        
        get("/{numericId}") {
            val numericId = call.parameters["numericId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val session = sessionService.getSession(numericId)
                ?: return@get call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
            
            call.respond(session.withoutAdminPassword())
        }
        
        post("/{numericId}/verify-admin") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val request = call.receive<VerifyAdminRequest>()
            val session = sessionService.getSession(numericId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
            
            val valid = sessionService.verifyAdmin(session, request.adminPassword)
            call.respond(VerifyAdminResponse(valid))
        }
        
        post("/{numericId}/players") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val request = call.receive<AddPlayerRequest>()
            
            try {
                val session = sessionService.addPlayer(numericId, request)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
                call.respond(session!!.withoutAdminPassword())
            } catch (e: IllegalAccessException) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", e.message ?: "Admin password required"))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("CONFLICT", e.message ?: "Cannot modify settled session"))
            }
        }
        
        post("/{numericId}/cash-out") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val request = call.receive<CashOutRequest>()
            
            try {
                val session = sessionService.cashOut(numericId, request)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
                call.respond(session!!.withoutAdminPassword())
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request"))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("CONFLICT", e.message ?: "Cannot cash out in settled session"))
            }
        }

        post("/{numericId}/cash-out/revoke") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))

            val request = call.receive<RevokeCashOutRequest>()

            try {
                val session = sessionService.revokeCashOut(numericId, request.playerId)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
                call.respond(session!!.withoutAdminPassword())
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request"))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("CONFLICT", e.message ?: "Cannot revoke in settled session"))
            }
        }

        post("/{numericId}/rebuy") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val request = call.receive<RebuyRequest>()
            
            try {
                val session = sessionService.rebuy(numericId, request)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
                call.respond(session!!.withoutAdminPassword())
            } catch (e: IllegalAccessException) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", e.message ?: "Admin password required"))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request"))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("CONFLICT", e.message ?: "Cannot modify settled session"))
            }
        }
        
        post("/{numericId}/settle") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val request = call.receive<SettleRequest>()
            
            try {
                val session = settlementService.settle(numericId, request)
                call.respond(session.withoutAdminPassword())
            } catch (e: IllegalAccessException) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", e.message ?: "Admin password required"))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request"))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("CONFLICT", e.message ?: "Session already settled"))
            }
        }
        
        post("/{numericId}/reopen") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val request = call.receiveOrNull<ReopenRequest>() ?: ReopenRequest()
            
            try {
                val session = settlementService.reopenSession(numericId, request.adminPassword)
                call.respond(session.withoutAdminPassword())
            } catch (e: IllegalAccessException) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", e.message ?: "Admin password required"))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request"))
            } catch (e: IllegalStateException) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse("CONFLICT", e.message ?: "Session is not settled"))
            }
        }
        
        post("/{numericId}/debts/settle") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val request = call.receive<ManualSettleRequest>()
            
            try {
                val session = settlementService.markDebtSettled(numericId, request)
                call.respond(session.withoutAdminPassword())
            } catch (e: IllegalAccessException) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", e.message ?: "Admin password required"))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request"))
            }
        }
        
        post("/{numericId}/diff") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val cashOuts = call.receive<Map<String, Long>>()
            
            val session = sessionService.getSession(numericId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
            
            val diff = settlementService.calculateDiff(session, cashOuts)
            call.respond(mapOf("diff" to diff))
        }
        
        post("/{numericId}/transfers") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val request = call.receive<AddTransferRequest>()
            
            try {
                var session = sessionService.addTransfer(numericId, request)
                    ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
                if (session!!.status == SessionStatus.SETTLED) {
                    session = settlementService.recalculateDebtsForSettledSession(numericId) ?: session
                }
                call.respond(session.withoutAdminPassword())
            } catch (e: IllegalAccessException) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", e.message ?: "Admin password required"))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request"))
            }
        }
        
        delete("/{numericId}/transfers/{transferId}") {
            val numericId = call.parameters["numericId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            val transferId = call.parameters["transferId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing transferId"))
            
            val adminPassword = call.request.header("X-Admin-Password")
                ?: try { call.receiveOrNull<AdminAuthBody>()?.adminPassword } catch (_: Exception) { null }
            
            try {
                var session = sessionService.removeTransfer(numericId, transferId, adminPassword)
                    ?: return@delete call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
                if (session!!.status == SessionStatus.SETTLED) {
                    session = settlementService.recalculateDebtsForSettledSession(numericId) ?: session
                }
                call.respond(session.withoutAdminPassword())
            } catch (e: IllegalAccessException) {
                call.respond(HttpStatusCode.Forbidden, ErrorResponse("FORBIDDEN", e.message ?: "Admin password required"))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message ?: "Invalid request"))
            }
        }
        
        post("/{numericId}/preview") {
            val numericId = call.parameters["numericId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing numericId"))
            
            val request = call.receive<SettleRequest>()
            
            val session = sessionService.getSession(numericId)
                ?: return@post call.respond(HttpStatusCode.NotFound, ErrorResponse("NOT_FOUND", "Session not found"))
            
            try {
                val previewDebts = settlementService.previewSettlement(session, request.cashOuts, request.balanceMode)
                call.respond(mapOf(
                    "debts" to previewDebts,
                    "transfers" to session.transfers
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", e.message ?: "Preview failed"))
            }
        }
    }
    
    route("/api/users") {
        get("/{userId}/stats") {
            val userId = call.parameters["userId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("BAD_REQUEST", "Missing userId"))
            
            val stats = sessionService.getUserStats(userId)
            call.respond(stats)
        }
    }
}
