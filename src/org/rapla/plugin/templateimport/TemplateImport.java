package org.rapla.plugin.templateimport;

import javax.jws.WebService;

import org.rapla.framework.RaplaException;
import org.rapla.rest.gwtjsonrpc.common.RemoteJsonService;

@WebService
public interface TemplateImport extends RemoteJsonService {
    public static final String BEGIN_KEY = "DatumVon";
	public static final String STORNO_KEY = "StorniertAm";
	public static final String PRIMARY_KEY = "Seminarnummer";
	public static final String TEMPLATE_KEY = "TitelName";

	ParsedTemplateResult importFromServer() throws RaplaException;

    boolean hasDBConnection() throws RaplaException;
}
