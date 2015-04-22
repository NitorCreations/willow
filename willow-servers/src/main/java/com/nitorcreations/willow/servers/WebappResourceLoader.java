package com.nitorcreations.willow.servers;

import java.io.InputStream;

import javax.servlet.ServletContext;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

public class WebappResourceLoader extends ResourceLoader {

    public static final String NAME = "webapp";

    private ServletContext servletContext;

    @Override
    public void init(ExtendedProperties configuration) {
        final String scClassName = ServletContext.class.getName();
        final Object sc = this.rsvc.getApplicationAttribute(scClassName);
        if (sc instanceof ServletContext) {
            this.servletContext = (ServletContext) sc;
        } else {
            this.log.error("Not found " + scClassName + ": " + sc);
        }
    }

    @Override
    public InputStream getResourceStream(final String source)
            throws ResourceNotFoundException {

        if (source == null || source.length() == 0) {
            throw new ResourceNotFoundException("No template name");
        }

        final InputStream res = this.servletContext
                .getResourceAsStream(source);
        if (res == null) {
            throw new ResourceNotFoundException(source);
        }
        return res;
    }

    @Override
    public boolean isSourceModified(Resource resource) {
        return resource.isSourceModified();
    }

    @Override
    public long getLastModified(Resource resource) {
        return resource.getLastModified();
    }
}
