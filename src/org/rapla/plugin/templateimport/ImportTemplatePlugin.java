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
package org.rapla.plugin.templateimport;
import org.rapla.client.ClientServiceContainer;
import org.rapla.client.RaplaClientExtensionPoints;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.TypedComponentRole;

// Plugin will be available in the 1.7 release
public class ImportTemplatePlugin  implements PluginDescriptor<ClientServiceContainer>
{
	public static final TypedComponentRole<I18nBundle> RESOURCE_FILE = new TypedComponentRole<I18nBundle>(ImportTemplatePlugin.class.getPackage().getName() + ".ImportTemplateResources");
    public static final String PLUGIN_CLASS = ImportTemplatePlugin.class.getName();
    public final static boolean ENABLE_BY_DEFAULT = false;

    public void provideServices(ClientServiceContainer container, Configuration config) throws RaplaContextException {
        if ( !config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT) )
        	return;

        container.addContainerProvidedComponent( RESOURCE_FILE, I18nBundleImpl.class,I18nBundleImpl.createConfig( RESOURCE_FILE.getId() ) );
        container.addContainerProvidedComponent( RaplaClientExtensionPoints.IMPORT_MENU_EXTENSION_POINT, ImportTemplatePluginInitializer.class);
    }

}