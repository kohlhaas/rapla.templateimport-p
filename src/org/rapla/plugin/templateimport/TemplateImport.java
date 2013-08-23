package org.rapla.plugin.templateimport;

import org.rapla.framework.RaplaException;

public interface TemplateImport {
    public static final String BEGIN_KEY = "DatumVon";
	public static final String STORNO_KEY = "StorniertAm";
	public static final String PRIMARY_KEY = "Seminarnummer";
	public static final String TEMPLATE_KEY = "TitelName";

	String importFromServer() throws RaplaException;

    boolean hasDBConnection() throws RaplaException;
}
