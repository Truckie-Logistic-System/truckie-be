package capstone_project.service.entityServices.auth.impl;


import capstone_project.entity.auth.UserEntity;
import capstone_project.repository.auth.UserRepository;
import capstone_project.service.entityServices.auth.UserEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserEntityServiceImpl implements UserEntityService {

    private final UserRepository userRepository;

    @Override
    public Optional<UserEntity> getUserById(UUID id) {
        return userRepository.findById(id);
    }

    @Override
    public Optional<UserEntity> getUserByUserName(final String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<UserEntity> getUserByUserNameOrEmail(final String username, final String email) {
        return userRepository.findByUsernameOrEmail(username, email);
    }

    @Override
    public Optional<UserEntity> getUserByEmail(final String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<UserEntity> getByUsernameWithRole(final String username) {
        return userRepository.findByUsernameWithRole(username);
    }

    @Override
    public UserEntity updateUserStatus(String email, String status) {
        return userRepository.updateUserStatus(email, status);
    }

    @Override
    public UserEntity save(UserEntity entity) {
        return userRepository.save(entity);
    }

    @Override
    public Optional<UserEntity> findEntityById(UUID uuid) {
        return userRepository.findById(uuid);
    }

    @Override
    public List<UserEntity> findAll() {
        return userRepository.findAll();
    }
}
