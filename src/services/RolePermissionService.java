package services;

import models.Permission;
import models.Role;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public class RolePermissionService {
    private final Map<Role, Set<Permission>> permissionsByRole = new EnumMap<>(Role.class);

    public RolePermissionService() {
        permissionsByRole.put(Role.VEHICLE_OWNER, EnumSet.of(
                Permission.VIEW_OWN_VEHICLES,
                Permission.REGISTER_VEHICLE));

        permissionsByRole.put(Role.JOB_SUBMITTER, EnumSet.of(
                Permission.SUBMIT_JOBS,
                Permission.TRACK_OWN_JOBS));

        permissionsByRole.put(Role.JOB_CONTROLLER, EnumSet.of(
                Permission.VIEW_ALL_JOBS,
                Permission.MANAGE_JOBS,
                Permission.ADJUST_JOB_QUEUE));

        permissionsByRole.put(Role.SYSTEM_ADMIN, EnumSet.allOf(Permission.class));
    }

    public boolean has(Role role, Permission permission) {
        Set<Permission> permissions = permissionsByRole.get(role);
        return permissions != null && permissions.contains(permission);
    }

    public Set<Permission> permissionsFor(Role role) {
        Set<Permission> permissions = permissionsByRole.get(role);
        if (permissions == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(permissions);
    }
}
