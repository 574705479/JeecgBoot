package org.jeecg.modules.airag.chat.repository;

import org.jeecg.modules.airag.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 聊天消息 MongoDB Repository
 *
 * @author jeecg
 * @date 2026-01-08
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 按会话ID查询消息（按时间升序）
     */
    List<ChatMessage> findByConversationIdAndDeletedIsFalseOrderByCreateTimeAsc(String conversationId);

    /**
     * 按会话ID分页查询消息（按时间降序）
     */
    Page<ChatMessage> findByConversationIdAndDeletedIsFalseOrderByCreateTimeDesc(String conversationId, Pageable pageable);

    /**
     * 按会话ID查询最新N条消息
     */
    List<ChatMessage> findTop100ByConversationIdAndDeletedIsFalseOrderByCreateTimeDesc(String conversationId);

    /**
     * 按用户ID和应用ID查询会话列表（去重）
     */
    @Query(value = "{ 'userId': ?0, 'appId': ?1, 'deleted': { $ne: true } }", 
           fields = "{ 'conversationId': 1, 'createTime': 1 }")
    List<ChatMessage> findDistinctConversationsByUserIdAndAppId(String userId, String appId);

    /**
     * 按外部用户ID和应用ID查询会话列表
     */
    @Query(value = "{ 'externalUserId': ?0, 'appId': ?1, 'deleted': { $ne: true } }",
           fields = "{ 'conversationId': 1, 'createTime': 1 }")
    List<ChatMessage> findDistinctConversationsByExternalUserIdAndAppId(String externalUserId, String appId);

    /**
     * 按应用ID查询所有活跃的AI会话
     */
    @Query(value = "{ 'appId': ?0, 'conversationType': 0, 'deleted': { $ne: true } }",
           fields = "{ 'conversationId': 1, 'userId': 1, 'externalUserId': 1, 'externalUserName': 1, 'createTime': 1 }")
    List<ChatMessage> findActiveAiConversationsByAppId(String appId);

    /**
     * 查询所有活跃的AI会话（最近有消息的）
     */
    @Query(value = "{ 'conversationType': 0, 'deleted': { $ne: true }, 'createTime': { $gte: ?0 } }")
    List<ChatMessage> findRecentAiConversations(Date since);

    /**
     * 统计会话消息数量
     */
    long countByConversationIdAndDeletedIsFalse(String conversationId);

    /**
     * 统计会话中某类型发送者的消息数量
     */
    long countByConversationIdAndSenderTypeAndDeletedIsFalse(String conversationId, Integer senderType);

    /**
     * 查询会话的最后一条消息
     */
    ChatMessage findTopByConversationIdAndDeletedIsFalseOrderByCreateTimeDesc(String conversationId);

    /**
     * 按会话ID删除所有消息（软删除）
     */
    @Query("{ 'conversationId': ?0 }")
    void softDeleteByConversationId(String conversationId);

    /**
     * 按时间范围查询消息
     */
    List<ChatMessage> findByConversationIdAndCreateTimeBetweenAndDeletedIsFalseOrderByCreateTimeAsc(
            String conversationId, Date startTime, Date endTime);

    /**
     * 查询未读消息数量
     */
    long countByConversationIdAndIsReadFalseAndSenderTypeAndDeletedIsFalse(
            String conversationId, Integer senderType);

    /**
     * 标记会话消息为已读
     */
    @Query("{ 'conversationId': ?0, 'senderType': ?1, 'isRead': false }")
    void markAsRead(String conversationId, Integer senderType);
}
