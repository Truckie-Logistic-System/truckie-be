package capstone_project.service.services.room.impl;

import capstone_project.common.enums.*;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.room.CreateRoomRequest;
import capstone_project.dtos.response.room.CreateRoomResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.chat.ParticipantInfo;
import capstone_project.entity.chat.RoomEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.service.mapper.room.RoomMapper;
import capstone_project.service.services.room.RoomService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomServiceImpl implements RoomService {
    private final Firestore firestore;
    private final RoomMapper roomMapper;
    private final UserEntityService userEntityService;
    private final OrderEntityService orderEntityService;
    private final ContractEntityService contractEntityService;


    @Override
    @Transactional
    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        if(request == null){
            log.warn("Input not found");
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        String roomType;
        List<ParticipantInfo> participantInfoList = new ArrayList<>();

        if (request.orderId() == null || request.orderId().isBlank()) {
            // -------- TH1: Customer chủ động tạo ----------
            roomType = RoomEnum.SUPPORT.name(); // hoặc FREE_CHAT

            // Chỉ có customerId trong participants
            UUID customerUuid = UUID.fromString(request.userIds().get(0));
            UserEntity customer = userEntityService.findEntityById(customerUuid)
                    .orElseThrow(() -> new BadRequestException("Customer not found", ErrorEnum.NOT_FOUND.getErrorCode()));

            ParticipantInfo cus = new ParticipantInfo();
            cus.setUserId(customer.getId().toString());
            cus.setRoleName(customer.getRole().getRoleName());
            participantInfoList.add(cus);

        }else {
            // -------- TH2: Chat theo Order ----------
            UUID orderUuid = UUID.fromString(request.orderId());
            OrderEntity order = orderEntityService.findEntityById(orderUuid)
                    .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));
            Optional<ContractEntity> contract = contractEntityService.getContractByOrderId(orderUuid);
            if(contract.isEmpty()){
                throw new NotFoundException("Contract not found", ErrorEnum.NOT_FOUND.getErrorCode());
            }

            if (!order.getStatus().equals(OrderStatusEnum.ON_PLANNING.name())) {
                throw new BadRequestException("Order status must be ON_PLANNING", ErrorEnum.INVALID_REQUEST.getErrorCode());
            }

            roomType = RoomEnum.ORDER_TYPE.name();

            // Lấy customer + staff trong order để đưa vào participants
            UUID customerId = order.getSender().getId();
            UUID staffId = contract.get().getStaff().getId();

            List<UUID> uuidList = Arrays.asList(customerId, staffId);
            Map<UUID, UserEntity> userMap = userEntityService.findAllByIdIn(uuidList)
                    .stream().collect(Collectors.toMap(UserEntity::getId, Function.identity()));

            for (UUID uid : uuidList) {
                UserEntity user = userMap.get(uid);
                ParticipantInfo p = new ParticipantInfo();
                p.setUserId(uid.toString());
                p.setRoleName(user.getRole().getRoleName());
                participantInfoList.add(p);
            }
        }
        // -------- Lưu vào Firestore ----------
        CollectionReference rooms = firestore.collection("Rooms");
        DocumentReference roomDoc = rooms.document();

        RoomEntity room = RoomEntity.builder()
                .roomId(roomDoc.getId())
                .orderId(request.orderId())
                .status(CommonStatusEnum.ACTIVE.name())
                .type(roomType)
                .participants(participantInfoList)
                .build();

        try {
            roomDoc.set(room);
            log.info("Room created with id: {}", room.getRoomId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to create room in Firestore", e);
        }

        return roomMapper.toCreateRoomResponse(room);
    }

    @Override
    public boolean cancelRoomByOrderId(UUID orderId) {
        if(orderId == null){
            log.warn("orderId not found");
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        try {
            CollectionReference rooms = firestore.collection("Rooms");
            Query query = rooms.whereEqualTo("orderId", orderId.toString());
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            if (documents.isEmpty()) {
                throw new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                );
            }
            for (QueryDocumentSnapshot doc : documents) {
                doc.getReference().update("status", CommonStatusEnum.INACTIVE.name());
                log.info("Room [{}] status set to UN_ACTIVE", doc.getId());
            }
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch room by orderId from Firestore", e);
        }
    }

    @Override
    public List<CreateRoomResponse> listRoomActiveByUserId(UUID userId) {
        if(userId == null){
            log.warn("userId not found");
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        List<RoomEntity> result = new ArrayList<>();
        try {
            CollectionReference rooms = firestore.collection("Rooms");
            Query query = rooms.whereEqualTo("status", CommonStatusEnum.ACTIVE.name());
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

            for (QueryDocumentSnapshot doc : documents) {
                RoomEntity room = doc.toObject(RoomEntity.class);
                boolean hasUser = room.getParticipants().stream()
                        .anyMatch(p -> p.getUserId().equals(userId.toString()));
                if (hasUser) {
                    result.add(room);
                }
            }
            return roomMapper.toCreateRoomResponseList(result);
        }catch (Exception e){
            throw new RuntimeException("Failed to fetch room by orderId from Firestore", e);
        }
    }

    @Override
    public List<CreateRoomResponse> getListSupportRoomsForStaff() {
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("Rooms")
                    .whereEqualTo("type", RoomEnum.SUPPORT.name())
                    .whereEqualTo("status", CommonStatusEnum.ACTIVE.name())
                    .get();

            List<QueryDocumentSnapshot> docs = future.get().getDocuments();

            List<RoomEntity> supportRooms = docs.stream()
                    .map(doc -> doc.toObject(RoomEntity.class))
                    .filter(room -> room.getParticipants() != null && room.getParticipants().size() == 1)
                    .toList();

            return roomMapper.toCreateRoomResponseList(supportRooms);

        } catch (Exception e) {
            log.error("Failed to get support rooms", e);
            throw new RuntimeException("Cannot get support rooms", e);
        }
    }


    @Override
    @Transactional
    public boolean joinRoom(String roomId, UUID staffId) {
        try {
            firestore.runTransaction(transaction -> {
                DocumentReference roomRef = firestore.collection("Rooms").document(roomId);
                DocumentSnapshot snapshot = transaction.get(roomRef).get();

                if (!snapshot.exists()) {
                    throw new BadRequestException("Room not found", ErrorEnum.NOT_FOUND.getErrorCode());
                }

                RoomEntity room = snapshot.toObject(RoomEntity.class);
                if (!RoomEnum.SUPPORT.name().equals(room.getType())) {
                    throw new BadRequestException("Room is not in SUPPORT state", ErrorEnum.INVALID_REQUEST.getErrorCode());
                }

                // Load staff info
                UserEntity staff = userEntityService.findEntityById(staffId)
                        .orElseThrow(() -> new BadRequestException("Staff not found", ErrorEnum.NOT_FOUND.getErrorCode()));


                ParticipantInfo staffParticipant = new ParticipantInfo();
                staffParticipant.setUserId(staff.getId().toString());
                staffParticipant.setRoleName(RoleTypeEnum.STAFF.name());

                List<ParticipantInfo> updatedParticipants = new ArrayList<>(room.getParticipants());
                updatedParticipants.add(staffParticipant);

                Map<String, Object> updates = new HashMap<>();
                updates.put("participants", updatedParticipants);
                updates.put("type", RoomEnum.SUPPORTED.name());

                transaction.update(roomRef, updates);

                return null; // Firestore transaction requires return
            }).get();

            log.info("Staff {} joined room {}", staffId, roomId);
            return true;

        } catch (BadRequestException e) {
            throw e; // rethrow domain exceptions
        } catch (Exception e) {
            log.error("Failed to join room", e);
            throw new RuntimeException("Cannot join room", e);
        }
    }


    @Override
    public boolean activeRoomByOrderId(UUID orderId) {
        if(orderId == null){
            log.warn("orderId not found");
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        try {
            CollectionReference rooms = firestore.collection("Rooms");
            Query query = rooms.whereEqualTo("orderId", orderId.toString());
            ApiFuture<QuerySnapshot> querySnapshot = query.get();
            List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();
            if (documents.isEmpty()) {
                throw new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                );
            }
            for (QueryDocumentSnapshot doc : documents) {
                doc.getReference().update("status", CommonStatusEnum.ACTIVE.name());
                log.info("Room [{}] status set to ACTIVE", doc.getId());
            }
            return true;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch room by orderId from Firestore", e);
        }
    }

    @Override
    public boolean deleteRoomByOrderId(UUID orderId) {
        return false;
    }
}
