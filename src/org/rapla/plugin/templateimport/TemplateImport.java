package org.rapla.plugin.templateimport;

import org.rapla.framework.RaplaException;

public interface TemplateImport {
    public static final String BEGIN_KEY = "DatumVon";

	String importFromServer() throws RaplaException;

    boolean hasDBConnection() throws RaplaException;
}
