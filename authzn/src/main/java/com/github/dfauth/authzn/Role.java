package com.github.dfauth.authzn;

public class Role {

    public static final String DEFAULT_SYSTEM_ID = "default";

    private String rolename;
    private String systemId;

    public Role(String systemId, String roleName) {
        this.rolename = roleName;
        this.systemId = systemId;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getRolename() {
        return rolename;
    }

    public static Role role(String role) {
        String systemId, roleName;
        String[] tmp = role.split(":");
        if(tmp.length != 2) {
            // default system
            systemId = DEFAULT_SYSTEM_ID;
            roleName = tmp[0];
        } else {
            systemId = tmp[0];
            roleName = tmp[1];
        }
        return new Role(systemId, roleName);
    }
}
