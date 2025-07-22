package capstone_project.service.entityServices.impl;


import capstone_project.entity.UsersEntity;
import capstone_project.repository.UserRepository;
import capstone_project.service.entityServices.UsersEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersEntityServiceImpl implements UsersEntityService {

    private final UserRepository userRepository;

    @Override
    public Optional<UsersEntity> getUserByUserName(final String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<UsersEntity> getUserByUserNameOrEmail(final String username, final String email) {
        return userRepository.findByUsernameOrEmail(username, email);
    }

    @Override
    public Optional<UsersEntity> getUserByEmail(final String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public Optional<UsersEntity> getByUsernameWithRole(final String username) {
        return userRepository.findByUsernameWithRole(username);
    }

    @Override
    public UsersEntity save(UsersEntity entity) {
        return userRepository.save(entity);
    }

    @Override
    public Optional<UsersEntity> findById(UUID uuid) {
        return userRepository.findById(uuid);
    }

    @Override
    public List<UsersEntity> findAll() {
        return userRepository.findAll();
    }
}
