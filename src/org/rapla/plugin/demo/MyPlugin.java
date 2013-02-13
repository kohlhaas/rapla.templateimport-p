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
package org.rapla.plugin.demo;
import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;

/**
   This is a demonstration of a rapla-plugin. It adds a sample usecase and option
   to the rapla-system.
 */

public class MyPlugin
    implements
    PluginDescriptor
{
	public static final String RESOURCE_FILE = MyPlugin.class.getPackage().getName() + ".MyPluginResources";
    public static final String PLUGIN_CLASS = MyPlugin.class.getName();

    public String toString() {
        return "MyPlugin";
    }

    /**
     * @see org.rapla.framework.PluginDescriptor#provideServices(org.rapla.framework.general.Container)
     */
    public void provideServices(Container container, Configuration config) {
        if ( !config.getAttributeAsBoolean("enabled", false) )
        	return;

        container.addContainerProvidedComponent( I18nBundle.ROLE, I18nBundleImpl.class.getName(), RESOURCE_FILE,I18nBundleImpl.createConfig( RESOURCE_FILE ) );
        container.addContainerProvidedComponent( RaplaExtensionPoints.CLIENT_EXTENSION, MyPluginInitializer.class.getName(), PLUGIN_CLASS, config);
        container.addContainerProvidedComponent( RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, MyOption.class.getName(),PLUGIN_CLASS, config);

        
    }

    public Object getPluginMetaInfos( String key )
    {
        return null;
    }

}

