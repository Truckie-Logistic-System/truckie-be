package capstone_project.service.services.user;

import capstone_project.dtos.response.user.DuplicateUserCleanupResponse;

/**
 * Service for cleaning up duplicate users in the system
 */
public interface UserCleanupService {
    
    /**
     * Find and remove duplicate users (users with the same username)
     * For each duplicate group, keep the first (oldest) user and delete the rest
     * Also deletes associated customer and driver records
     * 
     * @param dryRun if true, only report what would be deleted without actually deleting
     * @return Response containing details about deleted users
     */
    DuplicateUserCleanupResponse cleanupDuplicateUsers(boolean dryRun);
}
