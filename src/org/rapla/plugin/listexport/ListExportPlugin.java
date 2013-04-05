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
package org.rapla.plugin.listexport;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Configuration;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;

public class ListExportPlugin implements PluginDescriptor
{
	public static final String RESOURCE_FILE = ListExportPlugin.class.getPackage().getName() + ".ListExportResources";
    public static final String PLUGIN_CLASS = ListExportPlugin.class.getName();

    public String toString() {
        return "List Export";
    }
    
    /**
     * @see org.rapla.framework.PluginDescriptor#provideServices(org.rapla.framework.general.Container)
     */
    public void provideServices(Container container, Configuration config) {
        if ( !config.getAttributeAsBoolean("enabled", false) )
        	return;
        container.addContainerProvidedComponent( I18nBundle.class, I18nBundleImpl.class, RESOURCE_FILE,I18nBundleImpl.createConfig( RESOURCE_FILE ) );
        container.addContainerProvidedComponent( RaplaExtensionPoints.CLIENT_EXTENSION, ListExportPluginInitializer.class);
        //container.addContainerProvidedComponent( RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, DBExportOption.class.getName(),PLUGIN_CLASS, config);
    }

    public Object getPluginMetaInfos( String key )
    {
        return null;
    }

}