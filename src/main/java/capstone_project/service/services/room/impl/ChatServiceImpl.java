package capstone_project.service.services.room.impl;

import capstone_project.common.enums.*;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.room.ChatMessageDTO;
import capstone_project.dtos.request.room.MessageRequest;
import capstone_project.dtos.response.room.ChatPageResponse;
import capstone_project.dtos.response.room.ChatResponseDTO;
import capstone_project.entity.chat.ChatEntity;
import capstone_project.entity.chat.RoomEntity;
import capstone_project.service.mapper.room.ChatMapper;
import capstone_project.service.services.room.ChatService;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatServiceImpl implements ChatService {
    private final Firestore firestore;
    private final ChatMapper chatMapper;

    @Override
    public CompletableFuture<ChatResponseDTO> saveMessage(MessageRequest messageRequest) {
        if (messageRequest.roomId() == null || messageRequest.roomId().trim().isEmpty()
                || messageRequest.message() == null || messageRequest.message().trim().isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage()+"Không tìm thấy ID phòng hoặc nội dung tin nhắn chat",ErrorEnum.NOT_FOUND.getErrorCode());
        }



        // Reference tới subcollection messages của room tương ứng
        CollectionReference messagesRef = firestore.collection(FirebaseCollectionEnum.Rooms.name())
                .document(messageRequest.roomId())
                .collection(FirebaseCollectionEnum.Chats.name());

        // Tạo document mới (auto-generated ID)
        DocumentReference messageDoc = messagesRef.document();

        // Build ChatEntity để lưu
        ChatEntity chatEntity = ChatEntity.builder()
                .chatId(messageDoc.getId())
                .roomId(messageRequest.roomId())
                .senderId(messageRequest.senderId())
                .content(messageRequest.message())
                .type(MessageEnum.TEXT.name())
                .status(MessageEnum.SENT.name())
                .build();

        ApiFuture<WriteResult> future = messageDoc.set(chatEntity);

        // Convert ApiFuture -> CompletableFuture
        return convertToCompletableFuture(future)
                .thenApply(writeResult -> {
                    System.out.println("Message saved at: " + writeResult.getUpdateTime());
                    return chatMapper.toChatResponseDTO(chatEntity);
                })
                .exceptionally(ex -> {
                    System.err.println("Firestore Save Error: " + ex.getMessage());
                    throw new RuntimeException("Failed to save message", ex);
                });
    }

    @Override
    public ChatPageResponse getMessagesByRoomId(String roomId, int pageSize, String lastMessageId) throws ExecutionException, InterruptedException {
        CollectionReference messagesRef = firestore.collection(FirebaseCollectionEnum.Rooms.name())
                .document(roomId)
                .collection(FirebaseCollectionEnum.Chats.name());

        Query query = messagesRef.orderBy("createdAt", Query.Direction.DESCENDING).limit(pageSize);

        // Nếu có lastMessageId, startAfter document đó để paging
        if (lastMessageId != null && !lastMessageId.isEmpty()) {
            DocumentSnapshot lastDoc = messagesRef.document(lastMessageId).get().get();
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc);
            }
        }

        ApiFuture<QuerySnapshot> future = query.get();
        QuerySnapshot snapshot = future.get();

        List<ChatMessageDTO> messages = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            messages.add(new ChatMessageDTO(
                    doc.getId(),
                    doc.getString(FirebaseCollectionEnum.senderId.name()),
                    doc.getString(FirebaseCollectionEnum.content.name()),
                    doc.getTimestamp(FirebaseCollectionEnum.createdAt.name()),
                    doc.getString(FirebaseCollectionEnum.type.name())
            ));
        }

        String newLastMessageId = snapshot.getDocuments().isEmpty() ? null :
                snapshot.getDocuments().get(snapshot.size() - 1).getId();

        boolean hasMore = snapshot.size() == pageSize;

        return new ChatPageResponse(messages, newLastMessageId, hasMore);
    }

    @Override
    public ChatPageResponse getMessagesForRoomSupportForCusByUserId(UUID userId, int pageSize, String lastMessageId) throws ExecutionException, InterruptedException {
        if (userId == null) {
            throw new BadRequestException("UserId is required", ErrorEnum.NOT_FOUND.getErrorCode());
        }

        // 1️⃣ Lấy danh sách room SUPPORT mà user là participant
        CollectionReference roomsRef = firestore.collection(FirebaseCollectionEnum.Rooms.name());
        ApiFuture<QuerySnapshot> futureRooms = roomsRef
                .whereIn(FirebaseCollectionEnum.type.name(), List.of(RoomEnum.SUPPORT.name(), RoomEnum.SUPPORTED.name()))
                .whereEqualTo(FirebaseCollectionEnum.status.name(), CommonStatusEnum.ACTIVE.name())
                .get();

        List<QueryDocumentSnapshot> roomDocs = futureRooms.get().getDocuments();
        List<String> roomIds = roomDocs.stream()
                .map(doc -> doc.toObject(RoomEntity.class))
                .filter(room -> room.getParticipants().stream().anyMatch(p -> p.getUserId().equals(userId.toString())))
                .map(RoomEntity::getRoomId)
                .toList();

        if (roomIds.isEmpty()) {
            return new ChatPageResponse(new ArrayList<>(), null, false);
        }

        // 2️⃣ Lấy message từ tất cả room đó, orderBy createdAt DESC, paging
        CollectionReference messagesRef = firestore.collection(FirebaseCollectionEnum.Rooms.name())
                .document(roomIds.get(0)) // nếu muốn lấy theo room cụ thể, bạn có thể pass roomId vào param
                .collection(FirebaseCollectionEnum.Chats.name());

        Query query = messagesRef.orderBy(FirebaseCollectionEnum.createdAt.name(), Query.Direction.DESCENDING)
                .limit(pageSize);

        if (lastMessageId != null && !lastMessageId.isEmpty()) {
            DocumentSnapshot lastDoc = messagesRef.document(lastMessageId).get().get();
            if (lastDoc.exists()) {
                query = query.startAfter(lastDoc);
            }
        }

        ApiFuture<QuerySnapshot> futureMessages = query.get();
        QuerySnapshot snapshot = futureMessages.get();

        List<ChatMessageDTO> messages = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            messages.add(new ChatMessageDTO(
                    doc.getId(),
                    doc.getString(FirebaseCollectionEnum.senderId.name()),
                    doc.getString(FirebaseCollectionEnum.content.name()),
                    doc.getTimestamp(FirebaseCollectionEnum.createdAt.name()),
                    doc.getString(FirebaseCollectionEnum.type.name())
            ));
        }

        String newLastMessageId = snapshot.getDocuments().isEmpty() ? null :
                snapshot.getDocuments().get(snapshot.size() - 1).getId();

        boolean hasMore = snapshot.size() == pageSize;

        return new ChatPageResponse(messages, newLastMessageId, hasMore);
    }

    private <T> CompletableFuture<T> convertToCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        ApiFutures.addCallback(apiFuture, new ApiFutureCallback<T>() {
            @Override
            public void onFailure(Throwable t) {
                completableFuture.completeExceptionally(t);
            }
            @Override
            public void onSuccess(T result) {
                completableFuture.complete(result);
            }
        }, Executors.newSingleThreadExecutor());
        return completableFuture;
    }

}
