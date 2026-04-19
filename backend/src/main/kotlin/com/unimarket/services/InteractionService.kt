package com.unimarket.services

import com.unimarket.database.ChatMessages
import com.unimarket.database.ListingComments
import com.unimarket.database.Listings
import com.unimarket.database.Users
import com.unimarket.database.dbQuery
import com.unimarket.models.ChatMessageResponse
import com.unimarket.models.ConversationResponse
import com.unimarket.models.CreateCommentRequest
import com.unimarket.models.ListingCommentResponse
import com.unimarket.models.SellerNotificationResponse
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

object InteractionService {

    private fun userName(row: ResultRow): String =
        "${row[Users.firstName]} ${row[Users.lastName]}"

    private fun commentToResponse(row: ResultRow) = ListingCommentResponse(
        id = row[ListingComments.id].value,
        listingId = row[ListingComments.listingId],
        authorId = row[ListingComments.authorId],
        authorName = userName(row),
        authorRole = row[Users.role],
        parentCommentId = row[ListingComments.parentCommentId],
        message = row[ListingComments.message],
        createdAt = row[ListingComments.createdAt]
    )

    suspend fun getComments(listingId: Int): List<ListingCommentResponse> = dbQuery {
        ListingComments
            .join(Users, JoinType.INNER, ListingComments.authorId, Users.id)
            .selectAll()
            .where { ListingComments.listingId eq listingId }
            .orderBy(ListingComments.createdAt, SortOrder.ASC)
            .map(::commentToResponse)
    }

    suspend fun addComment(listingId: Int, authorId: Int, req: CreateCommentRequest): ListingCommentResponse {
        val text = req.message.trim()
        if (text.isBlank()) throw IllegalArgumentException("Comment cannot be empty")
        if (text.length > 500) throw IllegalArgumentException("Comment is too long")

        dbQuery {
            Listings.selectAll().where { Listings.id eq listingId }.firstOrNull()
                ?: throw NoSuchElementException("Listing not found")
        }

        val newId = dbQuery {
            ListingComments.insertAndGetId {
                it[ListingComments.listingId] = listingId
                it[ListingComments.authorId] = authorId
                it[parentCommentId] = req.parentCommentId
                it[message] = text
                it[createdAt] = System.currentTimeMillis()
            }.value
        }

        return dbQuery {
            ListingComments
                .join(Users, JoinType.INNER, ListingComments.authorId, Users.id)
                .selectAll()
                .where { ListingComments.id eq newId }
                .first()
                .let(::commentToResponse)
        }
    }

    suspend fun sellerComments(sellerId: Int): List<ListingCommentResponse> = dbQuery {
        ListingComments
            .join(Listings, JoinType.INNER, ListingComments.listingId, Listings.id)
            .join(Users, JoinType.INNER, ListingComments.authorId, Users.id)
            .selectAll()
            .where { Listings.sellerId eq sellerId }
            .orderBy(ListingComments.createdAt, SortOrder.DESC)
            .map(::commentToResponse)
    }

    private fun messageToResponse(row: ResultRow): ChatMessageResponse {
        val buyer = Users.selectAll().where { Users.id eq row[ChatMessages.buyerId] }.first()
        val seller = Users.selectAll().where { Users.id eq row[ChatMessages.sellerId] }.first()
        val buyerName = "${buyer[Users.firstName]} ${buyer[Users.lastName]}"
        val sellerName = "${seller[Users.firstName]} ${seller[Users.lastName]}"
        val senderId = row[ChatMessages.senderId]
        val senderName = if (senderId == row[ChatMessages.sellerId]) {
            sellerName
        } else {
            buyerName
        }

        return ChatMessageResponse(
            id = row[ChatMessages.id].value,
            listingId = row[ChatMessages.listingId],
            listingTitle = row[Listings.title],
            buyerId = row[ChatMessages.buyerId],
            buyerName = buyerName,
            sellerId = row[ChatMessages.sellerId],
            sellerName = sellerName,
            senderId = senderId,
            senderName = senderName,
            message = row[ChatMessages.message],
            createdAt = row[ChatMessages.createdAt],
            readAt = row[ChatMessages.readAt]
        )
    }

    suspend fun sendMessage(listingId: Int, currentUserId: Int, otherUserId: Int, message: String): ChatMessageResponse {
        val text = message.trim()
        if (text.isBlank()) throw IllegalArgumentException("Message cannot be empty")
        if (text.length > 800) throw IllegalArgumentException("Message is too long")

        val listing = dbQuery {
            Listings.selectAll().where { Listings.id eq listingId }.firstOrNull()
        } ?: throw NoSuchElementException("Listing not found")

        val sellerId = listing[Listings.sellerId]
        val buyerId = when {
            currentUserId == sellerId -> otherUserId
            otherUserId == sellerId -> currentUserId
            else -> throw IllegalAccessException("Chat must be between buyer and seller")
        }

        val newId = dbQuery {
            ChatMessages.insertAndGetId {
                it[ChatMessages.listingId] = listingId
                it[ChatMessages.buyerId] = buyerId
                it[ChatMessages.sellerId] = sellerId
                it[ChatMessages.senderId] = currentUserId
                it[ChatMessages.message] = text
                it[ChatMessages.createdAt] = System.currentTimeMillis()
                it[ChatMessages.readAt] = null
            }.value
        }

        return getMessage(newId)
    }

    suspend fun getThread(listingId: Int, currentUserId: Int, otherUserId: Int): List<ChatMessageResponse> {
        val listing = dbQuery {
            Listings.selectAll().where { Listings.id eq listingId }.firstOrNull()
        } ?: throw NoSuchElementException("Listing not found")
        val sellerId = listing[Listings.sellerId]
        val buyerId = if (currentUserId == sellerId) otherUserId else currentUserId

        dbQuery {
            ChatMessages.update({
                (ChatMessages.listingId eq listingId) and
                    (ChatMessages.buyerId eq buyerId) and
                    (ChatMessages.sellerId eq sellerId) and
                    (ChatMessages.senderId neq currentUserId) and
                    ChatMessages.readAt.isNull()
            }) {
                it[readAt] = System.currentTimeMillis()
            }
        }

        return dbQuery {
            ChatMessages
                .join(Listings, JoinType.INNER, ChatMessages.listingId, Listings.id)
                .join(Users, JoinType.INNER, ChatMessages.buyerId, Users.id)
                .selectAll()
                .where {
                    (ChatMessages.listingId eq listingId) and
                        (ChatMessages.buyerId eq buyerId) and
                        (ChatMessages.sellerId eq sellerId)
                }
                .orderBy(ChatMessages.createdAt, SortOrder.ASC)
                .map(::messageToResponse)
        }
    }

    suspend fun conversations(currentUserId: Int): List<ConversationResponse> = dbQuery {
        val userMessages = ChatMessages
            .join(Listings, JoinType.INNER, ChatMessages.listingId, Listings.id)
            .selectAll()
            .where { (ChatMessages.buyerId eq currentUserId) or (ChatMessages.sellerId eq currentUserId) }
            .toList()

        userMessages
            .groupBy { Triple(it[ChatMessages.listingId], it[ChatMessages.buyerId], it[ChatMessages.sellerId]) }
            .mapNotNull { (thread, rows) ->
                val latest = rows.maxByOrNull { it[ChatMessages.createdAt] } ?: return@mapNotNull null
                val latestAt = latest[ChatMessages.createdAt]
                val listing = Listings.selectAll().where { Listings.id eq thread.first }.firstOrNull()
                    ?: return@mapNotNull null
                val otherUserId = if (currentUserId == thread.third) thread.second else thread.third
                val otherUser = Users.selectAll().where { Users.id eq otherUserId }.firstOrNull()
                    ?: return@mapNotNull null
                val unread = rows.count {
                    it[ChatMessages.senderId] != currentUserId && it[ChatMessages.readAt] == null
                }

                ConversationResponse(
                    listingId = thread.first,
                    listingTitle = listing[Listings.title],
                    listingImageUrl = listing[Listings.imageUrl],
                    otherUserId = otherUserId,
                    otherUserName = "${otherUser[Users.firstName]} ${otherUser[Users.lastName]}",
                    lastMessage = latest[ChatMessages.message],
                    lastMessageAt = latestAt,
                    unreadCount = unread
                )
            }
            .sortedByDescending { it.lastMessageAt }
    }

    suspend fun sellerNotifications(sellerId: Int): SellerNotificationResponse = dbQuery {
        val commentCount = ListingComments
            .join(Listings, JoinType.INNER, ListingComments.listingId, Listings.id)
            .selectAll()
            .where { (Listings.sellerId eq sellerId) and (ListingComments.authorId neq sellerId) }
            .count()
            .toInt()

        val unreadMessages = ChatMessages
            .selectAll()
            .where {
                (ChatMessages.sellerId eq sellerId) and
                    (ChatMessages.senderId neq sellerId) and
                    ChatMessages.readAt.isNull()
            }
            .count()
            .toInt()

        SellerNotificationResponse(commentCount, unreadMessages)
    }

    private suspend fun getMessage(id: Int): ChatMessageResponse = dbQuery {
        ChatMessages
                .join(Listings, JoinType.INNER, ChatMessages.listingId, Listings.id)
            .selectAll()
            .where { ChatMessages.id eq id }
            .first()
            .let(::messageToResponse)
    }
}
