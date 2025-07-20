package capstone_project.service.entityServices.impl;


import capstone_project.entity.UsersEntity;
import capstone_project.repository.UserRepository;
import capstone_project.service.entityServices.UsersEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsersEntityServiceImpl implements UsersEntityService {

    private final UserRepository userRepository;

    @Override
    public UsersEntity createUser(UsersEntity usersEntity) {
        return userRepository.save(usersEntity);
    }

    @Override
    public Optional<UsersEntity> getUserById(final UUID id) {
        return userRepository.findById(id);
    }

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
}
