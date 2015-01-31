/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.templateimport.server;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContextException;
import org.rapla.plugin.templateimport.ImportTemplatePlugin;
import org.rapla.plugin.templateimport.TemplateImport;
import org.rapla.server.ServerServiceContainer;

// Plugin will be available in the 1.7 release
public class ImportTemplateServerPlugin  implements PluginDescriptor<ServerServiceContainer>
{

    public void provideServices(ServerServiceContainer container, Configuration config) throws RaplaContextException {
        if ( !config.getAttributeAsBoolean("enabled", ImportTemplatePlugin.ENABLE_BY_DEFAULT) )
        	return;

        container.addResourceFile( ImportTemplatePlugin.RESOURCE_FILE);
        container.addRemoteMethodFactory(TemplateImport.class,RaplaTemplateImport.class);
    }

}