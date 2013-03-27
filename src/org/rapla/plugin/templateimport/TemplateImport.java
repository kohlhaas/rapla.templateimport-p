package org.rapla.plugin.templateimport;

import org.rapla.framework.RaplaException;

public interface TemplateImport {
    String importFromServer() throws RaplaException;

    boolean hasDBConnection() throws RaplaException;
}
