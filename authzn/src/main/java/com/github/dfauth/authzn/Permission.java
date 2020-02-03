package com.github.dfauth.authzn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Permission {

    private static final Logger logger = LoggerFactory.getLogger(Permission.class);

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj == this) {
            return true;
        }
        if(getClass().equals(obj.getClass())) {
            return _equals(obj);
        }
        return false;
    }

    protected abstract boolean _equals(Object obj);

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public boolean implies(Permission permission, ResourceResolver resourceResolver) {
        if(permission == null) {
            logger.warn("null permission object passed to implies method of permission");
            return false;
        }
        if(permission == this) {
            logger.debug("identical permission comparison: "+this);
            return true;
        }
        return _implies(permission, resourceResolver);
    }

    protected abstract boolean _implies(Permission permission, ResourceResolver resourceResolver);

}
